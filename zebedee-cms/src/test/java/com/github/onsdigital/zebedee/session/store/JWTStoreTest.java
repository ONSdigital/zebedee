package com.github.onsdigital.zebedee.session.store;

import java.util.Map;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Test;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.github.onsdigital.impl.UserDataPayload;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThrows;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.notNullValue;

import com.github.onsdigital.zebedee.session.store.exceptions.SessionsRequestException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsDecodeException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsTokenExpiredException;
import com.github.onsdigital.zebedee.session.store.exceptions.SessionsVerificationException;

public class JWTStoreTest {

    private static final String REQUIRED_CLAIM_PAYLOAD_ERROR = "Required JWT payload claim not found [username or cognito:groups].";
    private static final String ACCESS_TOKEN_INTEGRITY_ERROR = "Verification of JWT token integrity failed.";
    private static final String RSA_PUBLIC_KEY_INALID_ERROR  = "Public Key cannot be empty or null.";


    private static final String SIGNED_TOKEN         = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9.fC3P6jnpnhmOxdlw0u4nOhehz7dCXsqX7RvqI1gEC4wrJoE6rlKH1mo7lR16K-EXWdXRoeN0_z0PZQzo__xOprAsY2XSNOexOcIo3hoydx6CkGWGmNNsLp35iGY3DgW6SLpQsdGF8HicJ9D9KCTPXKAGmOrkX3t92WSCLiQXXuER9gndzC6oLMU0akvKDstoTfwLWeSsogOQBn7_lUqGaHC8T06ZR37n_eOgUGSXwSFuYbg1zcY2xK3tMh4Wo8TOrADOrfLg660scpXuu-oDf0PNdgpXGU318IK1R0A2LiqqJWIV1sDE88uuPcX9-xgKc0eUn6qABXM9qhEyr6MS6g";
    private static final String UNSIGNED_TOKEN       = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCIsInVzZXJuYW1lIjoiamFuZWRvZUBleGFtcGxlLmNvbSJ9";
    private final static String TOKEN_NO_USER        = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6OTk5OTk5OTk5OSwiaWF0IjoxNTYyMTkwNTI0LCJqdGkiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJjbGllbnRfaWQiOiI1N2NiaXNoazRqMjRwYWJjMTIzNDU2Nzg5MCJ9.Vy6CJLdgDsCpoExm79aZh-2ugrO5u8M4M2g6s65-4RcocXxN5FZaQFvibwdh9h4bbz_qXqxJloBgZq3PmrIZrCIllmHhIbRmc3IISPG5_fdVspcjwVLUWLw-dWbdqaMo2uP6JIFmUx6DenO8ZB5I-82woyqhRxqfiCKG5q-ZEos4PzYO8bWcxYSOtC-j9p9bHJHxCUjwNvNHwSPUKrLacoo7e0dmpQI90PqK1KZqp52iieKdrHRYgHrmcTmiXY2mV2Ul8RodDl04jWvUwd52Qn4nIo-qUxROfnf5jbY1-rNotK-B3n5MSFA0YHcuiGN-bt8dUCyLLKkYjqBRpalzlg";
    private final static String TOKEN_EXPIRED_TIME   = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhYS1iYmJiLWNjY2MtZGRkZC1lZWVlZWVlZWVlZWUiLCJkZXZpY2Vfa2V5IjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY29nbml0bzpncm91cHMiOlsiYWRtaW4iLCJwdWJsaXNoaW5nIiwiZGF0YSIsInRlc3QiXSwidG9rZW5fdXNlIjoiYWNjZXNzIiwic2NvcGUiOiJhd3MuY29nbml0by5zaWduaW4udXNlci5hZG1pbiIsImF1dGhfdGltZSI6MTU2MjE5MDUyNCwiaXNzIjoiaHR0cHM6Ly9jb2duaXRvLWlkcC51cy13ZXN0LTIuYW1hem9uYXdzLmNvbS91cy13ZXN0LTJfZXhhbXBsZSIsImV4cCI6MTYyNTEzNzUzLCJpYXQiOjE1NjIxOTA1MjQsImp0aSI6ImFhYWFhYWFhLWJiYmItY2NjYy1kZGRkLWVlZWVlZWVlZWVlZSIsImNsaWVudF9pZCI6IjU3Y2Jpc2hrNGoyNHBhYmMxMjM0NTY3ODkwIiwidXNlcm5hbWUiOiJqYW5lZG9lQGV4YW1wbGUuY29tIn0.S-s8RJNTY2czRGpKLGmMb7fPgdmB086diMtc7M7eXPmjj4DCFkH0Pn9quqy3VEzPUp0NpKWmlVZlaZf0dyAhld7wIUYD8csMD7pMOE9zJMBw3elc9TZJnV06nA63-Htv_ykNvp-nuU1GzewIh_ujIV0RyPRbcxnxF8p2_kWuTnqvaZ6kt1M-XNuHt3lVDj9yAJFeApeZEdrB2-ma3sAsupHuvMQ2JFPvTKz0jWp_7oKi-O21M66TmBiNzpcZJFc7_S9oFHHuy0lW6C_kEI8yQMUPEewVhXwE6doJPHQj-v6j6xc0ieOyDwXpyRUmItapZyDTVF0hkawtw4h5vmvNNw";
    private final static String INVALID_SIGNED_TOKEN = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJOE09In0.eyJzdWIiOiJhYWFhYWFhLS1iYmJiLWNjY2MtZGPvv71kLWVlZWVlZWVlZWVlZSIsImPzv712aWNlX2tleSI6ImFhYWFhYWFhLWJiYmItY2NjYy1kZGRkLWVlZWVlZWVlZWVlZSIsImNvZ25pdG86Z3JvdXBzIjpbImFkbWluIiwicHVibGlzaGluZyIsImRhdGEiLCJ0ZXN0Il0sInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE1NjIxOTA1MjQsImlzcyI6Imh0dHBzOi8vY29nbml0by1pZHAudXMtd2VzdC0yLmFtYXpvbmF3cy5jb20vdXMtd2VzdC0yX2V4YW1wbGUiLCJleHAiOjk5OTk5OTk5OTksImlhdCI6MTU2MjE5MDUyNCwianRpIjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY2xpZW50X2lkIjoiNTdjYmlzaGs0ajI0cGFiYzEyMzQ1Njc4OTAiLCJ1c2VybmFtZSI6ImphbmVkb2VAZXhhbXBsZS5jb20ifQ.C5gKr5nVH1-ytJ9bAf996c5d7n1NLRsBi0wLYMoODJr36Kq-0fcQRC-l_2HAjnNmmj-FL8w8MJaIFkwt5rC4X5safj_LAELfJXgSukoDXhbeEZyyr5bG9gBkt92c7SKdyfHo19qNxZWt1b3xBAinrzBc9gv2khpII-Qc75s4FBOPiXIf1020gOirCW8Hzmj6Hpa2pCJHO4nuVqtJea-L8YVRw6Gc4d-DJQcDGH1tR8l1ynSI1e_8v8e0nNWkUxsBPAAUfb1jg61-YDCXylQhsEkLUfqSYNseRUtQMw2j4VEzHwu6f_P_g67-587Zp-FiATy_S2cwZdsIl_Ga9f0xrA";
    private final static String HEADER_KID_NOT_FOUND = "eyJhbGciOiJSUzI1NiIsImtpZCI6IjUxR3BvdHBTVGxtK3FjNXhOWUhzSko2S2tlT2JSZjlYSDQxYkhIS0JJWk09In0.eyJzdWIiOiJhYWFhYWFhLS1iYmJiLWNjY2MtZGPvv71kLWVlZWVlZWVlZWVlZSIsImPvv712aWNlX2tleSI6ImFhYWFhYWFhLWJiYmItY2NjYy1kZGRkLWVlZWVlZWVlZWVlZSIsImNvZ25pdG86Z3JvdXBzIjpbImFkbWluIiwicHVibGlzaGluZyIsImRhdGEiLCJ0ZXN0Il0sInRva2VuX3VzZSI6ImFjY2VzcyIsInNjb3BlIjoiYXdzLmNvZ25pdG8uc2lnbmluLnVzZXIuYWRtaW4iLCJhdXRoX3RpbWUiOjE1NjIxOTA1MjQsImlzcyI6Imh0dHBzOi8vY29nbml0by1pZHAudXMtd2VzdC0yLmFtYXpvbmF3cy5jb20vdXMtd2VzdC0yX2V4YW1wbGUiLCJleHAiOjk5OTk5OTk5OTksImlhdCI6MTU2MjE5MDUyNCwianRpIjoiYWFhYWFhYWEtYmJiYi1jY2NjLWRkZGQtZWVlZWVlZWVlZWVlIiwiY2xpZW50X2lkIjoiNTdjYmlzaGs0ajI0cGFiYzEyMzQ1Njc4OTAiLCJ1c2VybmFtZSI6ImphbmVkb2VAZXhhbXBsZS5jb20ifQ.anpazybyKL3RHUGqDFlSJKKj3OUVyoixfOsThR3yL_AuwVFyO7pXrur_nFOxmQvd4jGYCKZrx3DJa3GHNorbg2HghdV_THAIxzyDPq7mTB9yJTNHp8ewiWFltXtWRwaRBwnE23vbU7JNiAb2-SkD6uaemA_ZSO9RM3j4VNjeNmNXvXh2qO_yij_Z1clIDFu5gZr_XH7d5Vq5ie62BbxlHGP4Kv_aC6kZDOfca3tK02XBNaF_AtJAQa-Z9yUpmMWqbVwZEwjUwrZ2QxPx1-Yylj8G3tJTuFqur174RQaKY7Kd1ooP9ZvXXrtdhyKdeBMZguJg34rIEefc_CUV_1zXpw";

    private static final String RSA_KEY_ID_1      = "51GpotpSTlm+qc5xNYHsJJ6KkeObRf9XH41bHHKBI8M=";
    private static final String RSA_SIGNING_KEY_1 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzyis1ZjfNB0bBgKFMSv"
    +"vkTtwlvBsaJq7S5wA+kzeVOVpVWwkWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHc"
    +"aT92whREFpLv9cj5lTeJSibyr/Mrm/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIy"
    +"tvHWTxZYEcXLgAXFuUuaS3uF9gEiNQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0"
    +"e+lf4s4OxQawWD79J9/5d3Ry0vbV3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWb"
    +"V6L11BWkpzGXSW4Hv43qa+GSYOD2QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9"
    +"MwIDAQAB";

    private static final String RSA_KEY_ID_2      = "572VpotpSTlm+qc5xNYHsJJ6KkeObRf9XH41bbsYnb7Gf=";
    private static final String RSA_SIGNING_KEY_2 = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnzyis1ZjfNB0bBgKFMSv"
    +"vkTtwlvBsaJq7S5wA+kzeVOVpVWwkWdVha4s38XM/pa/yr47av7+z3VTmvDRyAHc"
    +"aT92whREFpLv9cj5lTeJSibyr/Mrm/YtjCZVWgaOYIhwrXwKLqPr/11inWsAkfIy"
    +"tvHWTxZYEcXLgAXFuUuaS3uF9gEiNQwzGTU1v0FqkqTBr4B8nW3HCN47XUu0t8Y0"
    +"e+lf4s4OxQawWD79J9/5d3Ry0vbV3Am1FtGJiJvOwRsIfVChDpYStTcHTCMqtvWb"
    +"V6L11BWkpzGXSW4Hv43qa+GSYOD2QU68Mb59oSk2OB+BtOLpJofmbGEGgvmwyCI9"
    +"MwIDAQAB";

    private JWTStore jwtStore;

    private Map<String, String> rsaKeyMap = new HashMap<String, String>();

    @Mock
    private UserDataPayload userDataPayload;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);

        rsaKeyMap.put(RSA_KEY_ID_1, RSA_SIGNING_KEY_1);
        rsaKeyMap.put(RSA_KEY_ID_2, RSA_SIGNING_KEY_2);

        this.jwtStore = new JWTStore(rsaKeyMap);
    }

    @Test
    public void decodeVerifyAndStoreAccessTokenData() throws Exception {
        this.jwtStore.set(SIGNED_TOKEN);
        ThreadLocal<UserDataPayload> tokenData = this.jwtStore.get();
        UserDataPayload localPayLoad = tokenData.get();
        Arrays.sort(localPayLoad.getGroups());
        assertThat(localPayLoad, is(notNullValue()));
        assertThat(localPayLoad.getEmail(), is("\"janedoe@example.com\""));
        assertThat(localPayLoad.getGroups()[0], is("admin"));
        assertThat(localPayLoad.getGroups()[1], is("data"));
        assertThat(localPayLoad.getGroups()[2], is("publishing"));
        assertThat(localPayLoad.getGroups()[3], is("test"));
    }   

    @Test
    public void decodeVerifyAndStoreAccessTokenDataThrowsTokenExcpetion() throws Exception {
        Exception exception = assertThrows(SessionsRequestException.class, () -> this.jwtStore.set(""));
        assertThat(exception.getMessage(), is(this.jwtStore.ACCESS_TOKEN_REQUIRED_ERROR));
    }

    @Test
    public void decodingAccessTokenWithNoUsernameInClaimsThrowsDecodeExcpetion() throws Exception {
        Exception exception = assertThrows(SessionsDecodeException.class, () -> this.jwtStore.set(TOKEN_NO_USER));
        assertThat(exception.getMessage(), is(REQUIRED_CLAIM_PAYLOAD_ERROR));
    }

    @Test
    public void decodedTokenIsExiredThrowsSessionsTokenExpiredException() throws Exception {
        Exception exception = assertThrows(SessionsTokenExpiredException.class, () -> this.jwtStore.set(TOKEN_EXPIRED_TIME));
        assertThat(exception.getMessage(), is(this.jwtStore.ACCESS_TOKEN_EXPIRED_ERROR));
    }

    @Test
    public void decodedTokenIsInvalidThrowsSessionVerificationException() throws Exception {
        Exception exception = assertThrows(SessionsVerificationException.class, () -> this.jwtStore.set(INVALID_SIGNED_TOKEN));
        assertThat(exception.getMessage(), is(ACCESS_TOKEN_INTEGRITY_ERROR));
    }

    @Test
    public void decodedTokenHeaderContainsKIDNotFoundInMapThrowsSessionsDecodeException() throws Exception {
        Exception exception = assertThrows(SessionsDecodeException.class, () -> this.jwtStore.set(HEADER_KID_NOT_FOUND));
        assertThat(exception.getMessage(), is(RSA_PUBLIC_KEY_INALID_ERROR));
    }

    @Test
    public void invalidlyFormattedAccessTokenPassedtoSetterThrowsSessionsDecodeException() throws Exception {
        Exception exception = assertThrows(SessionsDecodeException.class, () -> this.jwtStore.set(UNSIGNED_TOKEN));
        assertThat(exception.getMessage(), is(this.jwtStore.TOKEN_NOT_VALID_ERROR));
    }
}
