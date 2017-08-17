package org.apache.cordova.zsdk;

import org.apache.cordova.*;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.lang.RuntimeException;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;

import java.nio.charset.Charset;

import java.util.ArrayList;
import java.util.List;

import android.util.Base64;

import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveryHandler;
import com.zebra.sdk.printer.discovery.DiscoveryHandlerLinkOsOnly;
import com.zebra.sdk.printer.discovery.BluetoothDiscoverer;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.ZebraPrinterFactory;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;

import com.zebra.sdk.graphics.ZebraImageFactory;
import com.zebra.sdk.graphics.ZebraImageI;

public class ZsdkPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray data, final CallbackContext callbackContext) throws JSONException {

        if (action.equals("discover")) {

            DiscoveryHandler discoveryHandler = new DiscoveryHandler() {
                List<DiscoveredPrinter> printers = new ArrayList<DiscoveredPrinter>();
                JSONArray results = new JSONArray();

                public void foundPrinter(DiscoveredPrinter printer) {
                    try {
                        JSONObject result = new JSONObject();
                        if (printer.getDiscoveryDataMap().containsKey("FRIENDLY_NAME")) {
                            result.put("friendlyName", printer.getDiscoveryDataMap().get("FRIENDLY_NAME"));
                        }
                        if (printer.getDiscoveryDataMap().containsKey("MAC_ADDRESS")) {
                            result.put("macAddress", printer.getDiscoveryDataMap().get("MAC_ADDRESS"));
                        } else if (printer.address instanceof String) {
                            result.put("macAddress", printer.address);
                        }
                        results.put(result);
                    } catch (JSONException e) {
                      callbackContext.error("DISCOVERY_ERROR");
                      throw new RuntimeException(e);
                    }
                }

                public void discoveryFinished() {
                    callbackContext.success(results);
                }

                public void discoveryError(String message) {
                    callbackContext.error(message);
                }
            };
            try {
                BluetoothDiscoverer.findPrinters(this.cordova.getActivity().getApplicationContext(),
                    discoveryHandler);
            } catch (ConnectionException e) {
                callbackContext.error("CONNECTION_ERROR");
                callbackContext.error(e.getMessage());
            }

            return true;
        } else if (action.equals("printImage")) {

          String macAddress = data.getString(0);
          String rawImageData = data.getString(1);
          InputStream imageData = new ByteArrayInputStream(Base64.decode(rawImageData.getBytes(), Base64.DEFAULT));
          ZebraImageI image;
          try {
            image = ZebraImageFactory.getImage(imageData);
          } catch (IOException e) {
            callbackContext.error("IO_ERROR");
            throw new RuntimeException(e);
          }

          Connection connection = new BluetoothConnection(macAddress);
           try {
               connection.open();
               ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
               printer.printImage(image, 0, 0, 0, 0, false);
           } catch (ConnectionException e) {
               callbackContext.error("CONNECTION_ERROR");
               throw new RuntimeException(e);
           } catch (ZebraPrinterLanguageUnknownException e) {
               callbackContext.error("ZPLU_ERROR");
               throw new RuntimeException(e);
           } finally {
               try {
                 connection.close();
                 callbackContext.success("OK");
               } catch (ConnectionException e) {
                 callbackContext.error("CONNECTION_ERROR");
                 throw new RuntimeException(e);
               }
           }

           return true;
        } else {
            return false;
        }
    }
}
