package coupon.util;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class DistributedLockExecutor {

    private final RedissonClient redissonClient;
    private final DistributedLockTransactionExecutor transactionExecutor;

    @Around("@annotation(coupon.util.DistributedLock)")
    public void execute(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) proceedingJoinPoint.getSignature();
        Method method = signature.getMethod();

        DistributedLock distributedLock = method.getAnnotation(DistributedLock.class);
        Object[] args = proceedingJoinPoint.getArgs();
        Parameter[] parameters = method.getParameters();
        String lockName = resolveLockName(distributedLock.lockName(), parameters, args);

        RLock rLock = redissonClient.getLock(lockName);

        try {
            boolean canLock = rLock.tryLock(distributedLock.waitTime(), distributedLock.leaseTime(),
                    distributedLock.timeUnit());
            if (!canLock) {
                return;
            }
            transactionExecutor.execute(proceedingJoinPoint);
        } catch (InterruptedException e) {
            throw new RuntimeException(e.getMessage());
        } finally {
            rLock.unlock();
        }
    }

    private String resolveLockName(String lockNameTemplate, Parameter[] parameters, Object[] args) {
        for (int i = 0; i < parameters.length; i++) {
            String paramName = parameters[i].getName();
            lockNameTemplate = lockNameTemplate.replace("{" + paramName + "}", args[i].toString());
        }
        return lockNameTemplate;
    }
}
