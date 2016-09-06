<?php
/**
 * This component is used to authenticate users using an access token found
 * in the URL.
 * 
 * @author Gary Ng
 */
$query = filter_input_array(INPUT_GET);

if (isset($query['token']) && $query['token'] == ACCESS_TOKEN) {
    $token = $query['token'];
} else {
    $content = 'Access Denied';
    include INCLUDES_DIR . 'error.php';
    exit();
}
