<?php
error_reporting(E_ALL);
ini_set('display_errors', 1);
require_once '../../../supercaly/acra/main.php';
require_once INCLUDES_DIR . 'authenticate.php';

/**
 * This page is used to display the available crash reports. Crash reports
 * will be displayed in descending date received.
 * 
 * @author Gary Ng
 */
$query = filter_input_array(INPUT_GET);

if (isset($query['reload'])) {
    $xml = simplexml_load_file(REPORTS_XML_PATH);
    echo $xml->count(); // For Auto-Reloading Purposes
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
        <link rel="stylesheet" href="css/reports.css">
        <script src="js/script.js"></script>
    </head>
    <body>
        <div id="wrapper">
            <div id="header">
                <span class="title font">SuperCaly</span>
                <br />
                <span class="subtitle font">Crash Reports</span>
            </div>
            <div id="content">
                <?php include_once INCLUDES_DIR . 'reports.php'; ?>
            </div>
            <div id="footer">
                
            </div>
        </div>
    </body>
</html>
