package sample.context.security;

import java.util.*;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.*;

import lombok.Setter;
import sample.context.actor.Actor;

/**
 * Spring Securityで利用される認証/認可対象となるユーザ情報を提供します。
 */
@Setter
public class SecurityActorFinder {

    @Autowired
    private SecurityProperties props;
    @Autowired
    private ObjectProvider<SecurityUserService> userService;
    @Autowired
    private ObjectProvider<SecurityAdminService> adminService;

    /** 現在のプロセス状態に応じたUserDetailServiceを返します。 */
    public SecurityActorService detailsService() {
        return props.auth().isAdmin() ? adminService() : userService.getObject();
    }

    private SecurityAdminService adminService() {
        return Optional.ofNullable(adminService.getIfAvailable())
                .orElseThrow(() -> new IllegalStateException("SecurityAdminServiceをコンテナへBean登録してください。"));
    }

    /**
     * 現在有効な認証情報を返します。
     */
    public static Optional<Authentication> authentication() {
        return Optional.ofNullable(SecurityContextHolder.getContext().getAuthentication());
    }

    /**
     * 現在有効な利用者認証情報を返します。
     * <p>ログイン中の利用者情報を取りたいときはこちらを利用してください。
     */
    public static Optional<ActorDetails> actorDetails() {
        return authentication().map((auth) -> {
            return (auth.getDetails() instanceof ActorDetails) ? (ActorDetails) auth.getDetails() : null;
        });
    }

    /**
     * 認証/認可で用いられるユーザ情報。
     * <p>プロジェクト固有にカスタマイズしています。
     */
    public static class ActorDetails implements UserDetails {
        private static final long serialVersionUID = 1L;
        /** ログイン中の利用者情報 */
        private Actor actor;
        /** 認証パスワード(暗号化済) */
        private String password;
        /** 利用者の所有権限一覧 */
        private Collection<GrantedAuthority> authorities;

        public ActorDetails(Actor actor, String password, Collection<GrantedAuthority> authorities) {
            this.actor = actor;
            this.password = password;
            this.authorities = authorities;
        }

        public ActorDetails bindRequestInfo(HttpServletRequest request) {
            //low: L/B経由をきちんと考えるならヘッダーもチェックすること
            actor.setSource(request.getRemoteAddr());
            return this;
        }

        public Actor actor() {
            return actor;
        }

        @Override
        public String getUsername() {
            return actor.getId();
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public boolean isAccountNonExpired() {
            return true;
        }

        @Override
        public boolean isAccountNonLocked() {
            return true;
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true;
        }

        @Override
        public boolean isEnabled() {
            return true;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return authorities;
        }

        public Collection<String> getAuthorityIds() {
            return authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        }

    }

    /** Actorに適合したUserDetailsService */
    public static interface SecurityActorService extends UserDetailsService {
        /**
         * 与えられたログインIDを元に認証/認可対象のユーザ情報を返します。
         * @see org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
         */
        @Override
        public abstract ActorDetails loadUserByUsername(String username) throws UsernameNotFoundException;
    }

    /** 一般利用者向けI/F */
    public static interface SecurityUserService extends SecurityActorService {
    }

    /** 管理者向けI/F */
    public static interface SecurityAdminService extends SecurityActorService {
    }

}
