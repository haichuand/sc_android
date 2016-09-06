<?php
/**
 * This component is used to display the contents of a crash report.
 * 
 * @author Gary Ng
 */
?>
<div class="report">
    <h4>Report ID</h4>
    <pre><?php echo $xml->report_id; ?></pre>
    <h4>App Info</h4>
    <pre>
<?php
        echo sprintf('%-20s %s', 'App Name:', $xml->app_name);
        echo '<br />';
        echo sprintf('%-20s %s', 'Package Name:', $xml->package_name);
        echo '<br />';
        echo sprintf('%-20s %s', 'Version Code:', $xml->app_version_code);
        echo '<br />';
        echo sprintf('%-20s %s', 'Version Name:', $xml->app_version_name);
        echo '<br />';
        echo sprintf('%-20s %s', 'Build Date:', date('m-d-Y h:i:s A', $xml->build_date / 1000));
        echo '<br />';
        echo sprintf('%-20s %s', 'Installation ID:', $xml->installation_id);
?>
    </pre>
    <h4>Date & Time</h4>
    <pre>
<?php
        $startTime = strtotime($xml->user_app_start_date);
        $startDate = date('m-d-Y h:i:s A', $startTime);

        echo sprintf('%-20s %s', 'Date App Started:', $startDate);
        echo '<br />';

        $crashTime = strtotime($xml->user_crash_date);
        $crashDate = date('m-d-Y h:i:s A', $crashTime);

        echo sprintf('%-20s %s', 'Date of Crash:', $crashDate);
        echo '<br />';

        $duration = $crashTime - $startTime;
        if ($duration < 3) {
            $duration = '"Instant" Crash';
        } else {
            $duration = round($duration / 3600, 3) . ' Hours';
        }

        echo sprintf('%-20s %s', 'Duration:', $duration);
?>
    </pre>
    <h4>Device</h4>
    <pre>
<?php
        echo sprintf('%-20s %s', 'Brand:', $xml->brand);
        echo '<br />';
        echo sprintf('%-20s %s', 'Model:', $xml->phone_model);
        echo '<br />';
        echo sprintf('%-20s %s', 'Product:', $xml->product);
        echo '<br />';
        echo sprintf('%-20s %s', 'OS Version:', $xml->android_version);
?>
    </pre>
    <h4>User Comment</h4>
    <pre><?php echo $xml->user_comment == '' ? 'None' : $xml->user_comment ; ?></pre>
    <h4>Stack Trace</h4>
    <pre><?php echo $xml->stack_trace; ?></pre>
    <h4>Shared Preferences</h4>
    <pre><?php echo $xml->shared_preferences; ?></pre>
</div>
