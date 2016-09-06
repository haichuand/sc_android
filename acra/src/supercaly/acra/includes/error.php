<?php
/**
 * This page is used to display error messages.
 * 
 * @author Gary Ng
 */
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
    </head>
    <body>
        <div id="wrapper">
            <div id="header">
                <span class="title font">SuperCaly</span>
                <br />
                <span class="subtitle font">Error Message</span>
            </div>
            <div id="content">
                <?php echo isset($content) ? $content : 'You\'re in the right place!'; ?>
            </div>
            <div id="footer">
                
            </div>
        </div>
    </body>
</html>
