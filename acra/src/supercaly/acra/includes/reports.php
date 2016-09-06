<?php
/**
 * This component is used to display the listing of all available crash
 * reports.
 * 
 * @author Gary Ng
 */
const LIMIT = 50; // Max Crash Reports Limit

$xml = simplexml_load_file(REPORTS_XML_PATH);
$items = $xml->children();
$count = count($items);

if ($count > 0) {
    $min = min($count, LIMIT);
?>
<span class="count">Displaying <?php echo $min; ?> of <?php echo $count; ?> Reports</span>
<div class="list-group">
<?php
    $limit = $count - $min;
    for ($i = $count - 1; $i >= $limit; $i--) {
        $item = $items[$i];

        $time = (int) $item->time / 1000;
        if (date('Y-m-d', $time) == date('Y-m-d')) {
            $date = date('g:i A', $time);
        } else {
            $date = date('M j', $time);
        }
?>
    <a href="report.php?token=<?php echo $token; ?>&id=<?php echo $item['id']; ?>" class="list-group-item">
<?php
        if ($item->read != 'false') {
?>
        <span class="title nowrap col-xs-9 col-sm-10"><?php echo $item->title; ?></span>
<?php
        } else {
?>
        <span class="title nowrap col-xs-7 col-sm-9"><?php echo $item->title; ?></span>
        <span class="badge col-xs-2 col-sm-1">New</span>
<?php
        }
?>
        <span class="date col-xs-3 col-sm-2"><?php echo $date; ?></span>
    </a>
<?php
    }
?>
</div>
<?php
} else {
?>
<span>No Crashes, Yet!</span>
<?php
}
?>
<script>
    $(function() {
        var count = <?php echo $count; ?>;
        setInterval(function() {
            if ($(window).scrollTop() <= 0) {
                $.get('?token=<?php echo $token; ?>&reload=true', function(data) {
                    if (count !== parseInt(data)) {
                        location.reload();
                    }
                });
            }
        }, 10000);
    });
</script>
