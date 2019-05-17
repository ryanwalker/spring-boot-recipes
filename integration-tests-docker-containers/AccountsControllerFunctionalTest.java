package com.kubra.prepay.functional;


import static com.kubra.prepay.config.FunctionalTestingSecurityConfig.TEST_USERNAME_AND_PASSWORD;
import static org.junit.Assert.assertEquals;
import static org.springframework.http.HttpStatus.OK;

import com.kubra.prepay.generated.model.AccountListResource;
import com.kubra.prepay.generated.model.AccountResource;
import com.kubra.prepay.generated.model.AccountStatusResource;
import com.kubra.prepay.generated.model.AccountUpdateRequestResource;
import com.kubra.prepay.generated.model.MutableAccountFieldsResource;
import com.kubra.security.SecurityConstants;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

public class AccountsControllerFunctionalTest {

  public static final String ACCOUNTS_BASE_URL = "http://localhost:8080/v1/accounts";
  private RestTemplate template;
  private HttpHeaders headers;


  @Before
  public void setup() {
    template = new RestTemplate();

    //HttpClient is required for RestTemplate to be able to make PATCH requests
    HttpClient httpClient = HttpClientBuilder.create().build();
    template.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));

    headers = new HttpHeaders();
    headers.setContentType(MediaType.APPLICATION_JSON);
    headers.add(SecurityConstants.TENANT_HEADER, UUID.randomUUID().toString());
    headers.setBasicAuth(TEST_USERNAME_AND_PASSWORD, TEST_USERNAME_AND_PASSWORD);

  }

  @Test
  public void testHealth() throws URISyntaxException {
    URI url = new URI("http://localhost:8080/actuator/health");

    ResponseEntity<Map> response = template.getForEntity(url, Map.class);

    assertEquals(OK, response.getStatusCode());
  }

  @Test
  public void testCRUDAccount() throws URISyntaxException {
    // Register accounts
    ResponseEntity<AccountResource> registerResponse = registerAccount();
    assertEquals(OK, registerResponse.getStatusCode());

    // List accounts
    ResponseEntity<AccountListResource> getResponse = getAccounts();

    AccountListResource accounts = getResponse.getBody();

    assertEquals(OK, getResponse.getStatusCode());
    assertEquals(1, accounts.getCount().longValue());

    // Update accounts
    AccountResource accountResource = accounts.getAccounts().get(0);
    AccountUpdateRequestResource updateRequest = new AccountUpdateRequestResource();
    updateRequest.setServiceAddressLine1("1 W. First Ave.");
    updateRequest.setUpdateMask(Arrays.asList(MutableAccountFieldsResource.SERVICEADDRESSLINE1));

    ResponseEntity<AccountResource> updateResponse = updateAccount(accountResource, updateRequest);

    String serviceAddressLine1 = updateResponse.getBody().getServiceAddressLine1();
    assertEquals("1 W. First Ave.", serviceAddressLine1);

    // Delete account
    ResponseEntity deleteResponse = deleteAccount(accountResource.getAccountId());

    assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());

    // List accounts again
    getResponse = getAccounts();

    accounts = getResponse.getBody();

    assertEquals(OK, getResponse.getStatusCode());
    assertEquals(0, accounts.getCount().longValue());
  }

  private ResponseEntity<AccountResource> registerAccount() throws URISyntaxException {
    AccountResource requestBody = new AccountResource();
    requestBody.setAccountStatus(AccountStatusResource.CONNECTED);
    requestBody.setExternalAccountId(UUID.randomUUID().toString());

    HttpEntity<AccountResource> request = new HttpEntity<>(requestBody, headers);
    URI url = new URI(ACCOUNTS_BASE_URL);

    return template.postForEntity(url, request, AccountResource.class);
  }

  private ResponseEntity<AccountListResource> getAccounts() throws URISyntaxException {
    HttpEntity<AccountResource> request = new HttpEntity<>(headers);
    URI url = new URI(ACCOUNTS_BASE_URL);

    return template
        .exchange(url, HttpMethod.GET, request, AccountListResource.class);
  }

  private ResponseEntity<AccountResource> updateAccount(AccountResource accountResource,
      AccountUpdateRequestResource updateRequest)
      throws URISyntaxException {
    HttpEntity<AccountResource> request = new HttpEntity<>(updateRequest, headers);

    URI url = new URI(ACCOUNTS_BASE_URL + "/" + accountResource.getAccountId());

    return template.exchange(url, HttpMethod.PATCH, request, AccountResource.class);
  }

  private ResponseEntity deleteAccount(String accountId) throws URISyntaxException {
    URI url = new URI(ACCOUNTS_BASE_URL + "/" + accountId);
    HttpEntity<AccountResource> request = new HttpEntity<>(headers);

    return template
        .exchange(url, HttpMethod.DELETE, request, AccountResource.class);
  }

}
