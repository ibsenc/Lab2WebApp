import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.PrintWriter;
import javax.servlet.*;
import javax.servlet.http.*;
import javax.servlet.annotation.*;
import java.io.IOException;
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

  @Override
  protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
    res.setContentType("text/plain");
    String urlPath = req.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("missing parameters");
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
      sb.append(String.format("Resort ID: %s\n", urlParts[1]));
      sb.append(String.format("Season ID: %s\n", urlParts[3]));
      sb.append(String.format("Day ID: %s\n", urlParts[5]));
      sb.append(String.format("Skier ID: %s\n", urlParts[7]));

      res.getWriter().write(sb.toString());
    }
  }

  private boolean isUrlValid(String[] urlParts) {
    // TODO: validate the request url path according to the API spec
    // urlPath  = "/1/seasons/2019/day/1/skier/123"
    // urlParts = [, 1, seasons, 2019, day, 1, skier, 123]
    // From Swagger: "/{resortID}/seasons/{seasonID}/days/{dayID}/skiers/{skierID}"

    return (isInteger(urlParts[1]) &&
        urlParts[2].contains("season") &&
        isInteger(urlParts[3]) &&
        urlParts[4].contains("day") &&
        isInteger(urlParts[5]) &&
        urlParts[6].contains("skier")
        && isInteger(urlParts[7]));
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
    res.setContentType("text/plain");
    String urlPath = req.getPathInfo();

    // check we have a URL!
    if (urlPath == null || urlPath.isEmpty()) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("missing parameters");
      return;
    }

    String[] urlParts = urlPath.split("/");
    if (!isUrlValid(urlParts)) {
      res.setStatus(HttpServletResponse.SC_NOT_FOUND);
      res.getWriter().write("An invalid URL was provided.");
      return;
    }

    PrintWriter out = res.getWriter();
    try {
      StringBuilder sb = new StringBuilder();
      BufferedReader buffIn = req.getReader();
      String line;
      while( (line = buffIn.readLine()) != null) {
        sb.append(line);
      }

      if (!isValidJSON(sb.toString())) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write("Body must be valid JSON.");
        return;
      }

      LiftRide liftRide = null;
      try {
        liftRide = om.readValue(sb.toString(), LiftRide.class);
      } catch (Exception e) {
        System.out.println(e);
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write("Failed to deserialize json.");
        return;
      }

      String liftRideString = "";
      try {
        liftRideString = om.writeValueAsString(liftRide);
      } catch (JsonProcessingException e) {
        System.out.println(e);
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write("Could not write to object LiftRide.");
        return;
      }

      if (liftRide == null) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write("Invalid input.");
        return;
      }

      if (!isValidInteger(liftRide.getSkierID(), SKIER_ID_MIN, SKIER_ID_MAX)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(String.format(
            "skierID must be an integer between %d and %d.", SKIER_ID_MIN, SKIER_ID_MAX));
        return;
      }

      if (!isValidInteger(liftRide.getResortID(), RESORT_ID_MIN, RESORT_ID_MAX)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(String.format(
            "resortID must be an integer between %d and %d.", RESORT_ID_MIN, RESORT_ID_MAX));
        return;
      }

      if (!isValidInteger(liftRide.getLiftID(), LIFT_ID_MIN, LIFT_ID_MAX)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(String.format(
            "liftID must be an integer between %d and %d.", LIFT_ID_MIN, LIFT_ID_MAX));
        return;
      }

      if (!isValidString(liftRide.getSeasonID(), EXPECTED_SEASON_ID)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(
            String.format("seasonID must be %s for the current season.", EXPECTED_SEASON_ID));
        return;
      }

      if (!isValidString(liftRide.getDayID(), EXPECTED_DAY_ID)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(
            String.format("dayID must have a value of %s.", EXPECTED_DAY_ID));
        return;
      }

      if (!isValidInteger(liftRide.getTime(), TIME_MIN, TIME_MAX)) {
        res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        res.getWriter().write(String.format(
            "time must be an integer between %d and %d.", TIME_MIN, TIME_MAX));
        return;
      }

      res.setContentType("application/json");
      res.setCharacterEncoding("UTF-8");
      res.setStatus(HttpServletResponse.SC_OK);
      out.print(liftRideString);
      out.flush();
    }

    catch (Exception ex) {
      res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
      res.getWriter().write("Something went wrong!");
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
}
