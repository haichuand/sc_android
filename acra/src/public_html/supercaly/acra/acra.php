<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
require_once '../../../supercaly/acra/main.php';

/**
 * This component is used to generate a crash report with data produced from
 * the client using ACRA.
 * 
 * @author Gary Ng
 */
$data = filter_input_array(INPUT_POST);

if (!isset($data['REPORT_ID'])) {
    $content = 'There\'s nothing here...';
    include INCLUDES_DIR . 'error.php';
    exit();
}
// Custom Data -> Array
if (isset($data['CUSTOM_DATA'])) {
    $data['CUSTOM_DATA'] = parseCustomData($data['CUSTOM_DATA']);
}
// Generate Random Filename for Crash Report
$time = round(microtime(true) * 1000);
$filename = REPORTS_DIR . REPORT_XML_PREFIX . $time . '.xml';
// Create Crash Report
if (createCrashReportXML($data, $filename)) {
    updateReportsXML($time, $filename, $data);
}

/**
 * Convert custom data into associative array.
 */
function parseCustomData($data) {
    $result = array();

    $array = explode(PHP_EOL, $data);
    foreach ($array as $value) {
        if (trim($value) == '') {
            continue;
        }

        $pair = explode(' = ', $value);
        $result[$pair[0]] = $pair[1];
    }

    return $result;
}

/**
 * Replace value between prefix and suffix.
 */
function replaceBetween($str, $value, $prefix, $suffix = '') {
    $start = strpos($str, $prefix) + strlen($prefix) ;
    $end = $suffix != '' ? strpos($str, $suffix, $start) : strlen($str);
    $length = $end - $start;

    return $length > 0 ? substr_replace($str, $value, $start, $length) : $str;
}

/**
 * Create XML for crash report.
 */
function createCrashReportXML($data, $filename) {
    // Hide Sensitive Data
    if (isset($data['SHARED_PREFERENCES'])) {
        $str = $data['SHARED_PREFERENCES'];
        $value = '[HIDDEN]';

        $str = replaceBetween($str, $value, 'pref_gcm_token=', PHP_EOL);

        $array = array('username:', 'first_name:', 'last_name:', 'email:',
            'phone:');
        foreach ($array as $item) {
            $str = replaceBetween($str, $value, $item, ',');
        }

        $data['SHARED_PREFERENCES'] = $str;
    }
    // Create Crash Report XML
    $prolog = '<?xml version="1.0" encoding="UTF-8"?>';
    $xmlStr = $prolog .'<report></report>';

    $xml = new SimpleXMLElement($xmlStr);
    // Expected Data
    $keys = array(
        'REPORT_ID',
        'INSTALLATION_ID',
        'PACKAGE_NAME',
        'APP_VERSION_CODE',
        'APP_VERSION_NAME',
        'USER_APP_START_DATE',
        'USER_CRASH_DATE',
        'BRAND',
        'PHONE_MODEL',
        'PRODUCT',
        'ANDROID_VERSION',
        'SHARED_PREFERENCES',
        'STACK_TRACE',
        'USER_COMMENT'
    );
    // Handle Regular Data
    foreach ($keys as $key) {
        $xml->addChild(strtolower($key), $data[$key]);
    }
    // Handle Custom Data
    if (isset($data['CUSTOM_DATA'])) {
        $customKeys = array(
            'APP_NAME',
            'BUILD_DATE'
        );

        foreach ($customKeys as $key) {
            $xml->addChild(strtolower($key), $data['CUSTOM_DATA'][$key]);
        }
    }

    return $xml->asXML($filename);
}

/**
 * Update reports XML with reference to crash report.
 */
function updateReportsXML($time, $filename, $data) {
    $xml = simplexml_load_file(REPORTS_XML_PATH);

    $id = $data['REPORT_ID'];
    $installId = $data['INSTALLATION_ID'];
    $package = $data['PACKAGE_NAME'];
    $appName = $data['CUSTOM_DATA']['APP_NAME'];
    $version = $data['APP_VERSION_NAME'];
    $buildDate = $data['CUSTOM_DATA']['BUILD_DATE'];
    $brand = $data['BRAND'];
    $model = $data['PHONE_MODEL'];
    $osVersion = $data['ANDROID_VERSION'];

    $title = sprintf('v%s - %s %s on Android %s', $version, $brand, $model,
        $osVersion);

    $item = $xml->addChild('item');
    $item->addAttribute('id', $id);
    $item->addChild('title', $title);
    $item->addChild('time', $time);
    $item->addChild('install_id', $installId);
    $item->addChild('package', $package);
    $item->addChild('app_name', $appName);
    $item->addChild('version', $version);
    $item->addChild('build_date', $buildDate);
    $item->addChild('brand', $brand);
    $item->addChild('model', $model);
    $item->addChild('os_version', $osVersion);
    $item->addChild('path', $filename);
    $item->addChild('read', 'false');

    $xml->asXML(REPORTS_XML_PATH);
}
