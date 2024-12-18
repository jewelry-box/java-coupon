package coupon.util;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DistributedLockTransactionExecutor {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void execute(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        proceedingJoinPoint.proceed();
    }
}
