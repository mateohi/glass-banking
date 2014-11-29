package uy.infocorp.banking.glass.integration.privateapi.authentication;

import android.util.Log;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import java.io.UnsupportedEncodingException;
import java.util.List;

import uy.infocorp.banking.glass.R;
import uy.infocorp.banking.glass.integration.privateapi.PrivateUrls;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.authentication.SecurityDeviceValidationResult;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.authentication.SecurityQuestionsAnswers;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.authentication.SecurityQuestionsAnswersList;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.authentication.SignInInformation;
import uy.infocorp.banking.glass.integration.privateapi.common.dto.authentication.SignInResult;
import uy.infocorp.banking.glass.util.http.BaseClient;
import uy.infocorp.banking.glass.util.http.RestExecutionBuilder;
import uy.infocorp.banking.glass.util.resources.Resources;

public class AuthenticationClient extends BaseClient {

    private static final String TAG = AuthenticationClient.class.getSimpleName();
    private static final String X_AUTH_TOKEN_HEADER_NAME = Resources.getString(R.string.x_auth_header);

    private static AuthenticationClient instance;
    private RestExecutionBuilder builder;

    private AuthenticationClient() {
        builder = RestExecutionBuilder.post(PrivateUrls.POST_SIGN_IN_URL);
    }

    public static AuthenticationClient instance() {
        if (instance == null) {
            instance = new AuthenticationClient();
        }
        return instance;
    }

    private SignInResult logOnFirstStep(String username, String password) throws UnsupportedEncodingException {
        SignInInformation signInInformation = new SignInInformation(username, password);
        //initialize and execute post
        Pair<SignInResult, List<Header>> data = builder.appendObjectBody(signInInformation)
                .executeAndGetHeaders(SignInResult.class);

        SignInResult result = data.getLeft();
        List<Header> headers = data.getRight();

        //gets the header x_auth_token
        Header authToken = getAuthToken(headers);

        if (authToken != null) {
            result.setAuthToken(authToken.getValue());
        } else {
            Log.w(TAG, "No se pudo obtener el header X-Auth-Token al intentar loguearse");
        }

        return result;
    }

    private SecurityDeviceValidationResult validateSecurityDeviceSecondStep(SecurityQuestionsAnswers securityQuestionsAnswers,
                                                                            String signInAuthToken)
            throws UnsupportedEncodingException {

        SecurityQuestionsAnswersList request = new SecurityQuestionsAnswersList();
        request.getSecurityQuestionsAnswers().add(securityQuestionsAnswers);

        //do post
        BasicHeader header = new BasicHeader(X_AUTH_TOKEN_HEADER_NAME, signInAuthToken);
        Pair<SecurityDeviceValidationResult, List<Header>> data = builder.post(PrivateUrls.POST_VALIDATE_SECURITY_DEVICE_URL)
                .appendObjectBody(request)
                .appendHeader(header)
                .executeAndGetHeaders(SecurityDeviceValidationResult.class);

        SecurityDeviceValidationResult result = data.getLeft();
        List<Header> headers = data.getRight();

        //gets the header corresponding to the x_auth_token
        Header authToken = getAuthToken(headers);

        if (authToken != null) {
            result.setAuthToken(authToken.getValue());
        } else {
            Log.w(TAG, "No se pudo obtener el header X-Auth-Token al intentar validar el dispositivo de seguridad");
        }

        return result;
    }

    private static Header getAuthToken(List<Header> headers) {
        for (Header header : headers) {
            if (header.getName().equals(X_AUTH_TOKEN_HEADER_NAME)) {
                return header;
            }
        }
        return null;
    }

    @Override
    public Object getOffline() {
        return "test";
    }

    /**
     * Try to login and get the Authentication Token (executes both authentication steps)
     *
     * @return
     * @throws java.io.UnsupportedEncodingException
     */
    @Override
    public Object getOnline() {
        //1- LogOn
        SignInResult signInResult = null;
        try {
            signInResult = AuthenticationClient.instance().logOnFirstStep("prueba09", "1234");
        } catch (UnsupportedEncodingException e) {
            Log.e("Login First Step Error:", e.getMessage());
            return null;
        }
        //ToDo: Validar success en respuesta a login (usuario bloqueado, password invalido, etc)
        Integer secretQuestionId = Integer.parseInt(signInResult.getSignInInformation().getSecurityQuestionsToAnswerForLoginDevice().get(0).getSecurityQuestionId());
        SecurityQuestionsAnswers questionAnswered = new SecurityQuestionsAnswers(secretQuestionId, "1111");
        //2- Security Device Validation
        SecurityDeviceValidationResult securityDeviceValidationResult =
                null;
        try {
            securityDeviceValidationResult = AuthenticationClient.instance().validateSecurityDeviceSecondStep(questionAnswered,
                    signInResult.getAuthToken());
        } catch (UnsupportedEncodingException e) {
            Log.e("Login second Step Error:", e.getMessage());
            return null;
        }
        //ToDo: Validar success en respuesta a las preguntas de seguridad.
        return securityDeviceValidationResult.getAuthToken();
    }

    /**
     * Try to login and get the Authentication Token (executes both authentication steps)
     *
     * @return
     * @throws java.io.UnsupportedEncodingException
     */
    public String completeLogOn() throws UnsupportedEncodingException {
        return (String) (this.execute());
    }
}