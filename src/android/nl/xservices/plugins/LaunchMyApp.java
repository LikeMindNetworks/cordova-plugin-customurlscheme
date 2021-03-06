package nl.xservices.plugins;

import android.content.Intent;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaActivity;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Locale;

public class LaunchMyApp extends CordovaPlugin {

  private static final String ACTION_CHECKINTENT = "checkIntent";

  private static String externalString;

  /*
   * used to store external string from an external Java class method that
   * executes before "ondeviceready" event, for instance onNewIntent() from
   * MainActivity, so it can be used in execute() method -- this is to better
   * handle the case when external data is received upon app resuming from
   * Stopped/Hidden state
   */
  public static void setExternalString(String extStr) {
    externalString = extStr;
  }

  @Override
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    if (ACTION_CHECKINTENT.equalsIgnoreCase(action)) {
      final Intent intent = ((CordovaActivity) this.webView.getContext()).getIntent();
      final String intentString = intent.getDataString();
      final String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);

      if (intentString != null && intent.getScheme() != null) {
        // data from custom url
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, intent.getDataString()));
        intent.setData(null);
      } else if (extraText != null && externalString == null) {
        // text from share menu intent
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, extraText));
        intent.removeExtra(Intent.EXTRA_TEXT);
      } else if (externalString != null) {
        // text from external Java class method that executes before "ondeviceready" event
        callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, externalString));
        externalString = null;
      } else {
        callbackContext.error("App was not started via the launchmyapp URL scheme. Ignoring this errorcallback is the best approach.");
      }

      return true;
    } else {
      callbackContext.error("This plugin only responds to the " + ACTION_CHECKINTENT + " action.");
      return false;
    }
  }

  @Override
  public void onNewIntent(Intent intent) {
    final String intentString = intent.getDataString();
    final String extraText = intent.getStringExtra(Intent.EXTRA_TEXT);
    String outputString = null;

    if (intentString != null && intent.getScheme() != null) {
      // data from custom url
      intent.setData(null);

      outputString = intentString;
    } else if (extraText != null) {
      // text from share menu intent
      intent.removeExtra(Intent.EXTRA_TEXT);

      outputString = extraText;
    }

    if (outputString != null) {
      try {
        StringWriter writer = new StringWriter(outputString.length() * 2);
        escapeJavaStyleString(writer, outputString, true, false);

        webView.loadUrl("javascript:handleOpenURL('" + writer.toString() + "');");
      } catch (IOException ignore) {
      }
    }
  }

  // Taken from commons StringEscapeUtils
  private static void escapeJavaStyleString(Writer out, String str, boolean escapeSingleQuote,
                                            boolean escapeForwardSlash) throws IOException {
    if (out == null) {
      throw new IllegalArgumentException("The Writer must not be null");
    }
    if (str == null) {
      return;
    }
    int sz;
    sz = str.length();
    for (int i = 0; i < sz; i++) {
      char ch = str.charAt(i);

      // handle unicode
      if (ch > 0xfff) {
        out.write("\\u" + hex(ch));
      } else if (ch > 0xff) {
        out.write("\\u0" + hex(ch));
      } else if (ch > 0x7f) {
        out.write("\\u00" + hex(ch));
      } else if (ch < 32) {
        switch (ch) {
          case '\b':
            out.write('\\');
            out.write('b');
            break;
          case '\n':
            out.write('\\');
            out.write('n');
            break;
          case '\t':
            out.write('\\');
            out.write('t');
            break;
          case '\f':
            out.write('\\');
            out.write('f');
            break;
          case '\r':
            out.write('\\');
            out.write('r');
            break;
          default:
            if (ch > 0xf) {
              out.write("\\u00" + hex(ch));
            } else {
              out.write("\\u000" + hex(ch));
            }
            break;
        }
      } else {
        switch (ch) {
          case '\'':
            if (escapeSingleQuote) {
              out.write('\\');
            }
            out.write('\'');
            break;
          case '"':
            out.write('\\');
            out.write('"');
            break;
          case '\\':
            out.write('\\');
            out.write('\\');
            break;
          case '/':
            if (escapeForwardSlash) {
              out.write('\\');
            }
            out.write('/');
            break;
          default:
            out.write(ch);
            break;
        }
      }
    }
  }

  private static String hex(char ch) {
    return Integer.toHexString(ch).toUpperCase(Locale.ENGLISH);
  }
}
