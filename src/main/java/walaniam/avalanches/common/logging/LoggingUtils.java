package walaniam.avalanches.common.logging;

import com.microsoft.azure.functions.ExecutionContext;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.logging.Level;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LoggingUtils {

    public static void logInfo(ExecutionContext context, String messageTemplate, Object... params) {
        var log = context.getLogger();
        if (log.isLoggable(Level.INFO)) {
            var message = (params != null && params.length > 0)
                    ? String.format(messageTemplate, params)
                    : messageTemplate;
            message = String.format("[%s] %s", context.getFunctionName(), message);
            log.info(message);
        }
    }

    public static void logWarn(ExecutionContext context, String message, Throwable t) {
        var log = context.getLogger();
        message = String.format("[%s] %s", context.getFunctionName(), message);
        log.log(Level.WARNING, message);
        if (t != null) {
            t.printStackTrace();
        }
    }
}
