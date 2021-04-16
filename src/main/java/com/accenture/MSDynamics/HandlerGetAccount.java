package com.accenture.msdynamics;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

import com.accenture.msdynamics.entities.AuthorityEntity;
import com.accenture.msdynamics.entities.Details;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class HandlerGetAccount implements RequestHandler<HashMap<String, Object>, HashMap<String, String>> {
  Gson gson = new GsonBuilder().setPrettyPrinting().create();
  LambdaLogger logger = null;

  @Override
  public HashMap<String, String> handleRequest(HashMap<String, Object> event, Context context) {
    logger = context.getLogger();
    HashMap<String, String> response = new HashMap<String, String>();

    try {
      // logger.log("ENVIRONMENT VARIABLES: " + gson.toJson(System.getenv()));
      String DYNAMICS_ORG_URI = System.getenv().get("DYNAMICS_ORG_URI");
      String AZURE_TENANT_ID = System.getenv().get("AZURE_TENANT_ID");
      String CLIENT_ID = System.getenv().get("CLIENT_ID");
      String CLIENT_SECRET = System.getenv().get("CLIENT_SECRET");

      logger.log("CONTEXT: " + gson.toJson(context));

      logger.log("EVENT: " + event);
      logger.log("event.get(\"Details\").toString(): " + event.get("Details").toString());
      Details details = gson.fromJson(event.get("Details").toString().replace(":", "").replace("/", ""), Details.class);
      logger.log("details.Parameters.phone: " + details.Parameters.phone);

      AuthorityEntity accessToken = authenticate(DYNAMICS_ORG_URI, AZURE_TENANT_ID, CLIENT_ID, CLIENT_SECRET, context);

      return getAccountData(DYNAMICS_ORG_URI, accessToken.access_token, details.Parameters.phone, context);

    } catch (Exception ex) {
      response.put("ERRO", ex.getLocalizedMessage());
    }

    return response;
  }

  public AuthorityEntity authenticate(String organizationURI, String tenantId, String clientId, String clientSecret,
      Context context) throws Exception {

    logger = context.getLogger();

    HttpURLConnection connection = null;
    try {
      URL url = new URL("https://login.microsoft.com/" + tenantId + "/oauth2/token");
      connection = (HttpURLConnection) url.openConnection();
      connection.addRequestProperty("Content-Type", "application/x-www-form-urlencoded");
      connection.setRequestMethod("POST");
      connection.setDoOutput(true);

      String body = "grant_type=client_credentials" + "&client_id=" + clientId + "&client_secret=" + clientSecret
          + "&resource=" + organizationURI;
      OutputStream outputStream = connection.getOutputStream();
      outputStream.write(body.toString().getBytes());
      outputStream.close();

      StringBuilder response = new StringBuilder();
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        response.append(line);
      }
      bufferedReader.close();

      Gson gson = new Gson();
      AuthorityEntity auth = gson.fromJson(response.toString(), AuthorityEntity.class);
      logger.log("access_token: " + auth.access_token);

      return auth;

    } finally {
      if (connection != null)
        connection.disconnect();
    }
  }

  public HashMap<String, String> getAccountData(String OrganizationURI, String accessToken, String phone_number,
      Context context) throws Exception {

    logger = context.getLogger();

    HttpURLConnection connection = null;
    HashMap<String, String> response = new HashMap<String, String>();

    try {

      URL url = new URL(OrganizationURI + "api/data/v9.2/contacts?$select=contactid,fullname,telephone1&$filter="
          + URLEncoder.encode("telephone1 eq '" + phone_number + "'", StandardCharsets.UTF_8.name()));

      connection = (HttpURLConnection) url.openConnection();
      connection.addRequestProperty("Content-Type", "application/json; charset=utf-8");
      connection.setRequestProperty("Accept", "application/json");
      connection.setRequestProperty("Connection", "keep-alive");
      connection.addRequestProperty("Authorization", "Bearer " + accessToken);
      connection.setRequestMethod("GET");
      connection.setDoOutput(true);

      StringBuilder response_dynamics = new StringBuilder();
      BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        response_dynamics.append(line);
      }
      bufferedReader.close();
      logger.log("response_dynamics: " + response_dynamics.toString());

      //TODO: encontrar uma forma melhor de transformar o oData do MSDynamics em um objeto/classe
      String response_dynamics_lines[] = response_dynamics.toString().split(",");
      for (String s : response_dynamics_lines) {
        if (s.indexOf("contactid") != -1) {
          response.put("Id", s.replaceAll("\"", "").replace("fullname:", ""));
        } else if (s.indexOf("fullname") != -1) {
          response.put("Nome", s.replaceAll("\"", "").replace("fullname:", ""));
        } else if (s.indexOf("telephone1") != -1) {
          response.put("Telefone", s.replaceAll("\"", "").replace("fullname:", ""));
        }
      }

    } catch (Exception ex) {
      response.put("ERRO", ex.getLocalizedMessage());
    } finally {
      if (connection != null) {
        connection.disconnect();
      }
    }

    return response;
  }
}
