package com.mono;

import android.app.Application;
import android.content.Context;

import com.mono.settings.Settings;

import org.acra.ACRA;
import org.acra.ErrorReporter;
import org.acra.ReportingInteractionMode;
import org.acra.annotation.ReportsCrashes;

import static org.acra.ReportField.*;

@ReportsCrashes(
    formUri = "http://smurf.sfsu.edu/~wob/supercaly/acra/acra.php",
    customReportContent = {
        REPORT_ID, APP_VERSION_CODE, APP_VERSION_NAME, PACKAGE_NAME, PHONE_MODEL, ANDROID_VERSION,
        BRAND, PRODUCT, CUSTOM_DATA, STACK_TRACE, USER_COMMENT, USER_APP_START_DATE,
        USER_CRASH_DATE, INSTALLATION_ID, SHARED_PREFERENCES
    },
    mode = ReportingInteractionMode.DIALOG,
    reportDialogClass = CrashReportActivity.class,
    resDialogIcon = R.mipmap.ic_launcher,
    resDialogTitle = R.string.app_name,
    resDialogText = R.string.crash_report_text,
    resDialogCommentPrompt = R.string.crash_report_comment,
    resDialogPositiveButtonText = R.string.send_report,
    resDialogOkToast = R.string.crash_report_sent
)

/**
 * This application is primarily used to initialize ACRA for crash reporting.
 *
 * @author Gary Ng
 */
public class MainApplication extends Application {

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);

        ACRA.init(this);

        ErrorReporter errorReporter = ACRA.getErrorReporter();
        errorReporter.putCustomData("APP_NAME", getString(R.string.app_name));
        errorReporter.putCustomData("BUILD_DATE", String.valueOf(BuildConfig.BUILD_DATE));

        errorReporter.setEnabled(Settings.getInstance(this).getCrashReport());
    }
}
