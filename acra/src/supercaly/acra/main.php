<?php
/**
 * This file contains all essential global constants such as the root path,
 * path of directories, access token, etc.
 * 
 * @author Gary Ng
 */
define('SERVER_ROOT', '/project/wob/');
define('SITE_ROOT', SERVER_ROOT . 'public_html/supercaly/acra/');
define('ROOT', SERVER_ROOT . 'supercaly/acra/');

define('INCLUDES_DIR', ROOT . 'includes/');
define('REPORTS_DIR', ROOT . 'reports/'); // 747 (Permission)
define('XML_DIR', ROOT . 'xml/');

define('REPORTS_XML_PATH', XML_DIR . 'reports.xml'); // 646 (Permission)

define('REPORT_XML_PREFIX', 'report_');

define('ACCESS_TOKEN', '57cd0b8948552');
