package org.ibsenc;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ibsenc.exceptions.InvalidFieldException;
import org.ibsenc.exceptions.ParameterException;

@WebServlet(name = "org.ibsenc.SkierServlet", value = "/org.ibsenc.SkierServlet")
public class SkierServlet extends HttpServlet {
  private static final Integer SKIER_ID_MIN = 1;
  private static final Integer SKIER_ID_MAX = 100000;
  private static final Integer RESORT_ID_MIN = 1;
  private static final Integer RESORT_ID_MAX = 10;
  private static final Integer LIFT_ID_MIN = 1;
  private static final Integer LIFT_ID_MAX = 40;
  private static final String EXPECTED_SEASON_ID = "2022";
  private static final String EXPECTED_DAY_ID = "1";
  private static final Integer TIME_MIN = 1;
  private static final Integer TIME_MAX = 360;
  private static final Integer RESORT_ID_INDEX = 1;
  private static final Integer SEASON_ID_INDEX = 3;
  private static final Integer DAY_ID_INDEX = 5;
  private static final Integer SKIER_ID_INDEX = 7;
  private static final Integer SEASON_PARAM_INDEX = 2;
  private static final Integer DAY_PARAM_INDEX = 4;
  private static final Integer SKIER_PARAM_INDEX = 6;
  private static final Integer EXPECTED_PARAM_COUNT = 8;
  private ObjectMapper om;
  private String rabbitMQHostName;
  private RPCClient rpcClient;
  private ConnectionFactory factory;
  private Connection connection;
  private Channel channel;

  public void init() {
    om = new ObjectMapper();
    rabbitMQHostName = "localhost";

    factory = new ConnectionFactory();
    factory.setHost(rabbitMQHostName);

    try {
      connection = factory.newConnection();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
    try {
      channel = connection.createChannel();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      rpcClient = new RPCClient(connection, channel);
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (TimeoutException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("text/plain");
    String urlPath = req.getPathInfo();

    // Parse url, validate, and get parts
    String[] urlParts = getUrlParts(urlPath, res);
    if (urlParts == null) return;

    StringBuilder sb = new StringBuilder();
    sb.append("It works!\n\n");
    sb.append("Retrieving lift ride information for:\n");
    sb.append(String.format("Resort ID: %s\n", urlParts[RESORT_ID_INDEX]));
    sb.append(String.format("Season ID: %s\n", urlParts[SEASON_ID_INDEX]));
    sb.append(String.format("Day ID: %s\n", urlParts[DAY_ID_INDEX]));
    sb.append(String.format("Skier ID: %s\n", urlParts[SKIER_ID_INDEX]));

    res.setStatus(HttpServletResponse.SC_OK);
    res.getWriter().write(createJsonMessage(sb.toString()));
  }

  private boolean isUrlValid(String[] urlParts) {
    // From Swagger: "/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}"
    // example urlParts = [, 1, seasons, 2019, day, 1, skier, 123]

    if (urlParts.length != EXPECTED_PARAM_COUNT) {
      return false;
    }

    return (isInteger(urlParts[RESORT_ID_INDEX]) &&
        urlParts[SEASON_PARAM_INDEX].equals("seasons") &&
        isInteger(urlParts[SEASON_ID_INDEX]) &&
        urlParts[DAY_PARAM_INDEX].equals("days") &&
        isInteger(urlParts[DAY_ID_INDEX]) &&
        urlParts[SKIER_PARAM_INDEX].equals("skiers") &&
        isInteger(urlParts[SKIER_ID_INDEX]));
  }

  private boolean isInteger(String s) {
    if (s == null) return false;

    try {
      int integer = Integer.parseInt(s);
    } catch (NumberFormatException e) {
      return false;
    }

    return true;
  }

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse res)
      throws ServletException, IOException {
    res.setContentType("application/json");
    res.setCharacterEncoding("UTF-8");
    String urlPath = req.getPathInfo();

    // Parse url, validate, and get parts
    String[] urlParts = getUrlParts(urlPath, res);
    if (urlParts == null) return;

    PrintWriter out = res.getWriter();
    try {
      // Get JSON from request body
      String jsonPayload = getJson(req, res);
      if (jsonPayload == null) return;

      // Deserialize JSON into Object
      LiftRide liftRide = createLiftRide(jsonPayload, res);

      if (liftRide == null) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createJsonMessage("Invalid input."));
        return;
      }

      // Sets LiftRide fields from the given path parameters
      try {
        processPathParams(liftRide, urlParts);
      } catch (ParameterException e) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createJsonMessage(e.getMessage()));
        return;
      }

      // Validate LiftRide fields
      try {
        validateFields(liftRide);
      } catch (InvalidFieldException e) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createJsonMessage(e.getMessage()));
        return;
      }

      // Serialize Object back into JSON
      String liftRideString = getLiftRideAsJson(liftRide, res);
      if (liftRideString == null) return;

      // Add to queue
      String response = rpcClient.call(liftRideString);
      System.out.println("Response: " + response);

      // Send OK status and response
      res.setStatus(HttpServletResponse.SC_OK);
      out.print(liftRideString);
      out.flush();

    } catch (Exception ex) {
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      res.getWriter().write(createJsonMessage("Something went wrong!"));
      System.out.println(ex);
    }
  }

  // Reference: https://stackoverflow.com/questions/10226897/how-to-validate-json-with-jackson-json
  private boolean isValidJSON(final String json) {
    boolean validJson = false;
    try {
      try (JsonParser parser = new ObjectMapper().getFactory().createParser(json)) {
        while (parser.nextToken() != null) {
        }
      }
      validJson = true;
    } catch (JsonParseException jpe) {
      jpe.printStackTrace();
    } catch (IOException ioe) {
      ioe.printStackTrace();
    }

    return validJson;
  }

  private String[] getUrlParts(String urlPath, HttpServletResponse res) throws IOException {
    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(createJsonMessage("Missing parameters."));
      return null;
    }

    String[] urlParts = urlPath.split("/");
    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(createJsonMessage("An invalid URL was provided."));
      return null;
    }

    return urlParts;
  }

  private String getJson(HttpServletRequest req, HttpServletResponse res) throws IOException {
    StringBuilder sb = new StringBuilder();
    BufferedReader buffIn = req.getReader();
    String line;
    while( (line = buffIn.readLine()) != null) {
      sb.append(line);
    }

    if (!isValidJSON(sb.toString())) {
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write(createJsonMessage("Body must be valid JSON."));
      return null;
    }

    return sb.toString();
  }

  private LiftRide createLiftRide(String json, HttpServletResponse res) throws IOException {
    LiftRide liftRide;
    try {
      liftRide = om.readValue(json, LiftRide.class);
    } catch (Exception e) {
      System.out.println(e);
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write(createJsonMessage("Failed to deserialize json."));
      return null;
    }

    return liftRide;
  }

  private void processPathParams(LiftRide liftRide, String[] urlParts) throws IOException, ParameterException {
    try {
      liftRide.setResortID(Integer.parseInt(urlParts[RESORT_ID_INDEX]));
    } catch (NumberFormatException e) {
      throw new ParameterException("resortID could not be parsed to an integer.");
    }

    try {
      liftRide.setSkierID(Integer.parseInt(urlParts[SKIER_ID_INDEX]));
    } catch (NumberFormatException e) {
      throw new ParameterException("skierID could not be parsed to an integer.");
    }

    liftRide.setSeasonID(urlParts[SEASON_ID_INDEX]);
    liftRide.setDayID(urlParts[DAY_ID_INDEX]);
  }

  private void validateFields(LiftRide liftRide) throws IOException, InvalidFieldException {
    if (!isValidInteger(liftRide.getSkierID(), SKIER_ID_MIN, SKIER_ID_MAX)) {
      throw new InvalidFieldException(String.format(
          "skierID must be an integer between %d and %d.", SKIER_ID_MIN, SKIER_ID_MAX));
    }

    if (!isValidInteger(liftRide.getResortID(), RESORT_ID_MIN, RESORT_ID_MAX)) {
      throw new InvalidFieldException(String.format(
          "resortID must be an integer between %d and %d.", RESORT_ID_MIN, RESORT_ID_MAX));
    }

    if (!isValidInteger(liftRide.getLiftID(), LIFT_ID_MIN, LIFT_ID_MAX)) {
      throw new InvalidFieldException(String.format(
          "liftID must be an integer between %d and %d.", LIFT_ID_MIN, LIFT_ID_MAX));
    }

    if (!isValidString(liftRide.getSeasonID(), EXPECTED_SEASON_ID)) {
      throw new InvalidFieldException(String.format(
          "seasonID must be %s for the current season.", EXPECTED_SEASON_ID));
    }

    if (!isValidString(liftRide.getDayID(), EXPECTED_DAY_ID)) {
      throw new InvalidFieldException(String.format(
          "dayID must have a value of %s.", EXPECTED_DAY_ID));
    }

    if (!isValidInteger(liftRide.getTime(), TIME_MIN, TIME_MAX)) {
      throw new InvalidFieldException(String.format(
          "time must be an integer between %d and %d.", TIME_MIN, TIME_MAX));
    }
  }

  private String getLiftRideAsJson(LiftRide liftRide, HttpServletResponse res) throws IOException {
    String liftRideString;
    try {
      liftRideString = om.writeValueAsString(liftRide);
    } catch (JsonProcessingException e) {
      System.out.println(e);
      res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      res.getWriter().write(createJsonMessage("Could not write to object LiftRide."));
      return null;
    }

    return liftRideString;
  }

  private boolean isValidInteger(Integer value, int low, int high) {
    return value != null && value >= low && value <= high;
  }

  private boolean isValidString(String s, String expectedValue) {
    return s != null && !s.isEmpty() && !s.trim().isEmpty() && s.equals(expectedValue);
  }

  private String createJsonMessage(String message) {
    return String.format("{ \"message\": \"%s\" }", message);
  }
}
