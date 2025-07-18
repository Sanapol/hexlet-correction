package io.hexlet.typoreporter.service.oauth2;

import io.hexlet.typoreporter.domain.account.AuthProvider;
import io.hexlet.typoreporter.service.AccountService;
import io.hexlet.typoreporter.service.account.signup.SignupAccount;
import io.hexlet.typoreporter.utils.TextUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OAuth2Service implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AccountService accountService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oAuth2User = new DefaultOAuth2UserService().loadUser(userRequest);
        String oAuth2Provider = userRequest.getClientRegistration().getRegistrationId().toUpperCase();
        String accessToken = userRequest.getAccessToken().getTokenValue();

        OAuth2UserInfo oAuth2UserInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(
            oAuth2Provider, accessToken, new HashMap<>(oAuth2User.getAttributes()));

        String email = oAuth2UserInfo.getEmail();

        if (email == null) {
            throw new OAuth2AuthenticationException("Email from provider " + oAuth2Provider + " not received");
        }

        String normalizedEmail = TextUtils.toLowerCaseData(email);

        if (!accountService.existsByEmail(normalizedEmail)) {
            var newAccount = new SignupAccount(
                oAuth2UserInfo.getUsername(),
                normalizedEmail,
                "OAUTH2_USER",
                oAuth2UserInfo.getFirstName(),
                oAuth2UserInfo.getLastName(),
                AuthProvider.valueOf(oAuth2Provider).name()
            );
            accountService.signup(newAccount);
        }

        Map<String, Object> oAuth2UserAttributes = oAuth2UserInfo.getAttributes();
        oAuth2UserAttributes.putIfAbsent("email", normalizedEmail);

        return new CustomOAuth2User(
            oAuth2User.getAuthorities(),
            oAuth2UserAttributes,
            "email",
            oAuth2UserInfo.getUsername()
        );
    }
}
