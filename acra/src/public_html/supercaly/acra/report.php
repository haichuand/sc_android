<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
require_once '../../../supercaly/acra/main.php';
require_once INCLUDES_DIR . 'authenticate.php';

/**
 * This page is used to display a crash report.
 * 
 * @author Gary Ng
 */
$query = filter_input_array(INPUT_GET);

if (isset($query['id']) && $query['id'] != '') {
    $xml = simplexml_load_file(REPORTS_XML_PATH);
    $items = $xml->xpath('//item[@id="' . $query['id'] . '"]');

    if (count($items) > 0) {
        $item = $items[0];
        // Mark as Read
        $item->read = 'true';
        $xml->asXML(REPORTS_XML_PATH);
        // Retrieve Crash Report XML
        $xml = simplexml_load_file($item->path);
    } else {
        $xml = null;
    }
}

if (!isset($xml)) {
    $content = 'Are you sure you know what you\'re looking for?';
    include INCLUDES_DIR . 'error.php';
    exit();
}
?>
<!DOCTYPE html>
<html>
    <head>
        <meta charset="UTF-8">
        <meta name="author" content="Gary Ng">
        <meta name="viewport" content="width=device-width, initial-scale=1">
        <title>SuperCaly</title>

        <script src="https://ajax.googleapis.com/ajax/libs/jquery/1.12.4/jquery.min.js"></script>

        <link rel="stylesheet" href="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css">
        <script src="http://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/js/bootstrap.min.js"></script>

        <link rel="stylesheet" href="https://fonts.googleapis.com/css?family=Roboto:100">

        <link rel="stylesheet" href="css/style.css">
        <link rel="stylesheet" href="css/report.css">
        <script src="js/script.js"></script>
    </head>
    <body>
        <div id="wrapper">
            <div id="header">
                <span class="title font">SuperCaly</span>
                <br />
                <span class="subtitle font">Viewing Crash Report</span>
            </div>
            <div id="content">
                <?php include_once INCLUDES_DIR . 'report.php'; ?>
            </div>
            <div id="footer">
                
            </div>
        </div>
    </body>
</html>
