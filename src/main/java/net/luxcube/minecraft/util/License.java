package net.luxcube.minecraft.util;


import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.bukkit.Bukkit;
import org.bukkit.command.ConsoleCommandSender;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;


public class License {

  private static final String URL = "https://license.pikzstudio.xyz";
  private static final String PRODUCT = "DonutHomes";

  private static final ConsoleCommandSender logger = Bukkit.getConsoleSender();

  public static boolean isLicenseValid(String id) {
    if (!isAlive()) {
      logger.sendMessage("§4------------------");
      logger.sendMessage("§4------------------");
      logger.sendMessage("§4PLEASE CONTACT THE AUTHOR ON https://discord.gg/pikzstudios");
      logger.sendMessage("§4NOTE: THE PLUGIN HAS BEEN ENABLED.");
      logger.sendMessage("§4------------------");
      logger.sendMessage("§4------------------");
      return true;
    }

    LicenseData licenseData = getLicenseById(id);

    if (licenseData == null || !licenseData.product.equals(PRODUCT)) {

      logger.sendMessage("§4------------------");
      logger.sendMessage("§4------------------");
      logger.sendMessage("§4INVALID LICENSE KEY '" + id + "'");
      logger.sendMessage("§4");
      logger.sendMessage("§4PLEASE CONTACT THE AUTHOR ON https://discord.gg/pikzstudios");
      logger.sendMessage("§4TO GET YOUR KITS LICENSE KEY.");
      logger.sendMessage("§4------------------");
      logger.sendMessage("§4------------------");
      return false;
    }


    SimpleDateFormat dateFormat = new SimpleDateFormat();

    logger.sendMessage("§a-------------------");
    logger.sendMessage("§fLicense Key: §2" + id.substring(0, id.length() / 5) + "**********");
    logger.sendMessage("§fOwner: §2" + licenseData.owner);
    logger.sendMessage("§fCreation Date: §2" + dateFormat.format(new Date(licenseData.creation_date)));
    logger.sendMessage("§fProduct: §2" + licenseData.product);
    logger.sendMessage("§fAllowed IPs: §2" + Arrays.toString(licenseData.ips));
    logger.sendMessage("§a-------------------");

    return true;
  }

  public static boolean isAlive() {

    HttpClient httpClient = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .connectTimeout(Duration.ofSeconds(10))
      .build();

    try {

      HttpRequest request = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create(URL))
        .setHeader("User-Agent", "Licensing System") // add request header
        .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      return response.statusCode() == 200;
    } catch (Exception e) {
      e.printStackTrace();
      return false;
    }

  }

  public static LicenseData getLicenseById(String id) {
    String requestStr = "/license/id/" + id;

    HttpClient httpClient = HttpClient.newBuilder()
      .version(HttpClient.Version.HTTP_2)
      .connectTimeout(Duration.ofSeconds(10))
      .build();

    try {

      HttpRequest request = HttpRequest.newBuilder()
        .GET()
        .uri(URI.create(URL + requestStr))
        .setHeader("User-Agent", "Licensing System") // add request header
        .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

      if (response.statusCode() != 200)
        return null;

      JsonObject jsonObject = new JsonParser().parse(response.body()).getAsJsonObject();

      String owner = jsonObject.get("owner").getAsString();
      String product = jsonObject.get("product").getAsString();
      long creationDate = jsonObject.get("creation_date").getAsLong();
      List<String> ips = new ArrayList<>();
      jsonObject.get("ips").getAsJsonArray().forEach(jsonElement -> ips.add(jsonElement.getAsString()));
      return new LicenseData(id, owner, product, creationDate, ips.toArray(String[]::new));

    } catch (Exception e) {
      return null;
    }

  }


  public record LicenseData(String id, String owner, String product, long creation_date, String[] ips) {
  }

}

