package sample.controller;

import org.aspectj.lang.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import sample.context.actor.*;
import sample.context.actor.Actor.ActorRoleType;

/**
 * Spring Securityの設定状況に応じてスレッドローカルへ利用者を紐付けるAOPInterceptor。
 */
@Aspect
@Configuration
public class LoginInterceptor {
    
    @Autowired
    ActorSession session;

    @Before("execution(* *..controller.system.*Controller.*(..))")
    public void bindSystem() {
        session.bind(Actor.System);
    }

    @After("execution(* *..controller..*Controller.*(..))")
    public void unbind() {
        session.unbind();
    }

    /**
     * セキュリティの認証設定(extension.security.auth.enabled)が無効時のみ有効される擬似ログイン処理。
     * <p>開発時のみ利用してください。
     */
    @Aspect
    @Component
    @ConditionalOnProperty(name = "extension.security.auth.enabled", havingValue = "false", matchIfMissing = false)
    public static class DummyLoginInterceptor {
        @Autowired
        ActorSession session;

        @Before("execution(* *..controller.*Controller.*(..))")
        void bindUser() {
            session.bind(new Actor("sample", ActorRoleType.User));
        }

        @Before("execution(* *..controller.admin.*Controller.*(..))")
        void bindAdmin() {
            session.bind(new Actor("admin", ActorRoleType.Internal));
        }
    }

}
