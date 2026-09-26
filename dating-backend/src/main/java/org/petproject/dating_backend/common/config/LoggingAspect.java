package org.petproject.dating_backend.common.config;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Component
@Aspect
@Slf4j
public class LoggingAspect {

    @Around("execution(* org.petproject.dating_backend.user.UserService.*(..)) || "
            + "execution(* org.petproject.dating_backend.auth.AuthController.*(..)) ||"
            + "execution(* org.petproject.dating_backend.photo.PhotoService.*(..))")
    public Object logAround(ProceedingJoinPoint pjp) throws Throwable{
        String method = pjp.getSignature().toShortString();
        Object[] args = pjp.getArgs();

        if (args.length < 7) {
            log.info("→ {} | args={}", method, Arrays.toString(args));
        } else log.info("→ {} with more then 7 args", method);

        long start = System.currentTimeMillis();

        try{
            Object result = pjp.proceed();
            log.info("← {} | time={}ms | result={}",
                    method, System.currentTimeMillis() - start, result);
            return result;
        } catch (Throwable th) {
            log.warn("✕ {} | time={}ms | error={}",
                    method, System.currentTimeMillis() - start, th.getMessage());
            throw th;
        }

    }

}
