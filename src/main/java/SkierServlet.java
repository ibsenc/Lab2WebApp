import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.ibsenc.LiftRide;

@WebServlet(name = "SkierServlet", value = "/SkierServlet")
public class SkierServlet extends HttpServlet {
  private ObjectMapper om = new ObjectMapper();
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

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("text/plain");
    String urlPath = req.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(createErrorMessage("missing parameters"));
      return;
    }

    String[] urlParts = urlPath.split("/");

    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
    } else {
      res.setStatus(HttpServletResponse.SC_OK);
      StringBuilder sb = new StringBuilder();
      sb.append("It works!\n\n");
      sb.append("Retrieving lift ride information for:\n");
      sb.append(String.format("Resort ID: %s\n", urlParts[RESORT_ID_INDEX]));
      sb.append(String.format("Season ID: %s\n", urlParts[SEASON_ID_INDEX]));
      sb.append(String.format("Day ID: %s\n", urlParts[DAY_ID_INDEX]));
      sb.append(String.format("Skier ID: %s\n", urlParts[SKIER_ID_INDEX]));

      res.getWriter().write(createErrorMessage(sb.toString()));
    }
  }

  private boolean isUrlValid(String[] urlParts) {
    // TODO: validate the request url path according to the API spec
    // urlPath  = "/1/seasons/2019/day/1/skier/123"
    // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
    // From Swagger: "/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}"

    return (isInteger(urlParts[RESORT_ID_INDEX]) &&
        urlParts[SEASON_PARAM_INDEX].contains("season") &&
        isInteger(urlParts[SEASON_ID_INDEX]) &&
        urlParts[DAY_PARAM_INDEX].contains("day") &&
        isInteger(urlParts[DAY_ID_INDEX]) &&
        urlParts[SKIER_PARAM_INDEX].contains("skier")
        && isInteger(urlParts[SKIER_ID_INDEX]));
  }

  private boolean isInteger(String s) {
    if (s == null) {
      return false;
    }

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

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(createErrorMessage("missing parameters"));
      return;
    }

    String[] urlParts = urlPath.split("/");
    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write(createErrorMessage("An invalid URL was provided."));
      return;
    }

    PrintWriter out = res.getWriter();
    try {

      // Process request body

      StringBuilder sb = new StringBuilder();
      BufferedReader buffIn = req.getReader();
      String line;
      while( (line = buffIn.readLine()) != null) {
        sb.append(line);
      }

      if (!isValidJSON(sb.toString())) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage("Body must be valid JSON."));
        return;
      }

      // Deserialize JSON into Object

      LiftRide liftRide = null;
      try {
        liftRide = om.readValue(sb.toString(), LiftRide.class);
      } catch (Exception e) {
        System.out.println(e);
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage("Failed to deserialize json."));
        return;
      }

      if (liftRide == null) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage("Invalid input."));
        return;
      }

      // Process path parameters

      try {
        liftRide.setResortID(Integer.parseInt(urlParts[RESORT_ID_INDEX]));
      } catch (NumberFormatException e) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage("resortID could not be parsed to an integer."));
        return;
      }

      try {
        liftRide.setSkierID(Integer.parseInt(urlParts[SKIER_ID_INDEX]));
      } catch (NumberFormatException e) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage("skierID could not be parsed to an integer."));
        return;
      }

      liftRide.setSeasonID(urlParts[SEASON_ID_INDEX]);
      liftRide.setDayID(urlParts[DAY_ID_INDEX]);

      // Validate the LiftRide fields

      if (!isValidInteger(liftRide.getSkierID(), SKIER_ID_MIN, SKIER_ID_MAX)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage(String.format(
            "skierID must be an integer between %d and %d.", SKIER_ID_MIN, SKIER_ID_MAX)));
        return;
      }

      if (!isValidInteger(liftRide.getResortID(), RESORT_ID_MIN, RESORT_ID_MAX)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage(String.format(
            "resortID must be an integer between %d and %d.", RESORT_ID_MIN, RESORT_ID_MAX)));
        return;
      }

      if (!isValidInteger(liftRide.getLiftID(), LIFT_ID_MIN, LIFT_ID_MAX)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage(String.format(
            "liftID must be an integer between %d and %d.", LIFT_ID_MIN, LIFT_ID_MAX)));
        return;
      }

      if (!isValidString(liftRide.getSeasonID(), EXPECTED_SEASON_ID)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage(
            String.format("seasonID must be %s for the current season.", EXPECTED_SEASON_ID)));
        return;
      }

      if (!isValidString(liftRide.getDayID(), EXPECTED_DAY_ID)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage(
            String.format("dayID must have a value of %s.", EXPECTED_DAY_ID)));
        return;
      }

      if (!isValidInteger(liftRide.getTime(), TIME_MIN, TIME_MAX)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage(String.format(
            "time must be an integer between %d and %d.", TIME_MIN, TIME_MAX)));
        return;
      }

      // Serialize Object back into JSON

      String liftRideString = "";
      try {
        liftRideString = om.writeValueAsString(liftRide);
      } catch (JsonProcessingException e) {
        System.out.println(e);
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(createErrorMessage("Could not write to object LiftRide."));
        return;
      }

      res.setStatus(HttpServletResponse.SC_OK);
      out.print(liftRideString);
      out.flush();
    }

    catch (Exception ex) {
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      res.getWriter().write(createErrorMessage("Something went wrong!"));
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

  private boolean isValidInteger(Integer value, int low, int high) {
    return value != null && value >= low && value <= high;
  }

  private boolean isValidString(String s, String expectedValue) {
    return s != null && !s.isEmpty() && !s.trim().isEmpty() && s.equals(expectedValue);
  }

  private String createErrorMessage(String message) {
    return String.format("{ \"message\": \"%s\" }", message);
  }
}
