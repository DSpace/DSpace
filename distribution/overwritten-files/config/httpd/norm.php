<?php
if (get_magic_quotes_gpc()) {
    $process = array(&$_GET, &$_POST, &$_COOKIE, &$_REQUEST);
    while (list($key, $val) = each($process)) {
        foreach ($val as $k => $v) {
            unset($process[$key][$k]);
            if (is_array($v)) {
                $process[$key][stripslashes($k)] = $v;
                $process[] = &$process[$key][stripslashes($k)];
            } else {
                $process[$key][stripslashes($k)] = stripslashes($v);
            }
        }
    }
    unset($process);
}

function special_htmlentities ($utf2html_string)
{
    $f = 0xffff;
    $convmap = array(
/* <!ENTITY % HTMLlat1 PUBLIC "-//W3C//ENTITIES Latin 1//EN//HTML">
    %HTMLlat1; */
    38, 38, 0, $f,  60,  60, 0, $f, 62,  62, 0, $f,  
    //160,  255, 0, $f,
/* <!ENTITY % HTMLsymbol PUBLIC "-//W3C//ENTITIES Symbols//EN//HTML">
    %HTMLsymbol; */
     402,  402, 0, $f,  913,  929, 0, $f,  931,  937, 0, $f,
     945,  969, 0, $f,  977,  978, 0, $f,  982,  982, 0, $f,
    8226, 8226, 0, $f, 8230, 8230, 0, $f, 8242, 8243, 0, $f,
    8254, 8254, 0, $f, 8260, 8260, 0, $f, 8465, 8465, 0, $f,
    8472, 8472, 0, $f, 8476, 8476, 0, $f, 8482, 8482, 0, $f,
    8501, 8501, 0, $f, 8592, 8596, 0, $f, 8629, 8629, 0, $f,
    8656, 8660, 0, $f, 8704, 8704, 0, $f, 8706, 8707, 0, $f,
    8709, 8709, 0, $f, 8711, 8713, 0, $f, 8715, 8715, 0, $f,
    8719, 8719, 0, $f, 8721, 8722, 0, $f, 8727, 8727, 0, $f,
    8730, 8730, 0, $f, 8733, 8734, 0, $f, 8736, 8736, 0, $f,
    8743, 8747, 0, $f, 8756, 8756, 0, $f, 8764, 8764, 0, $f,
    8773, 8773, 0, $f, 8776, 8776, 0, $f, 8800, 8801, 0, $f,
    8804, 8805, 0, $f, 8834, 8836, 0, $f, 8838, 8839, 0, $f,
    8853, 8853, 0, $f, 8855, 8855, 0, $f, 8869, 8869, 0, $f,
    8901, 8901, 0, $f, 8968, 8971, 0, $f, 9001, 9002, 0, $f,
    9674, 9674, 0, $f, 9824, 9824, 0, $f, 9827, 9827, 0, $f,
    9829, 9830, 0, $f,
/* <!ENTITY % HTMLspecial PUBLIC "-//W3C//ENTITIES Special//EN//HTML">
   %HTMLspecial; */
/* These ones are excluded to enable HTML: 34, 38, 60, 62 */
     338,  339, 0, $f,  352,  353, 0, $f,  376,  376, 0, $f,
     710,  710, 0, $f,  732,  732, 0, $f, 8194, 8195, 0, $f,
    8201, 8201, 0, $f, 8204, 8207, 0, $f, 8211, 8212, 0, $f,
    8216, 8218, 0, $f, 8218, 8218, 0, $f, 8220, 8222, 0, $f,
    8224, 8225, 0, $f, 8240, 8240, 0, $f, 8249, 8250, 0, $f,
    8364, 8364, 0, $f);

    return mb_encode_numericentity($utf2html_string, $convmap, "UTF-8");
}

$remove_whitespaces = empty($_POST['remove_whitespaces']);
$remove_enters = empty($_POST['remove_enters']);
$remove_multiple_enters = empty($_POST['remove_multiple_enters']);

if (isset($_POST['text'])){
	$text = $_POST['text'];
//	$text2 = htmlspecialchars(special_htmlentities($text),ENT_NOQUOTES);
	$text2 = htmlspecialchars(htmlspecialchars($text,ENT_NOQUOTES));

	if ($remove_multiple_enters)
		$text2 = preg_replace("/[\r\n]+/", "\n", $text2);
	if ($remove_enters)
		$text2 = preg_replace("/([^\.:])\n/", '$1 ', $text2);
	if ($remove_whitespaces)
		$text2 = trim(preg_replace("/\h+/", ' ', $text2));

}else{
	$text = "";
	$text2 = '';
}

?>
<html>
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=utf-8"/>

</head>
<body>

<form action="norm.php" method="POST">
	Ingrese el texto a transformar 
	<br/>
	<textarea name="text" cols="100" rows="15"><?php echo $text?></textarea>
	<br/>
	<input type="checkbox" <?php if (!$remove_whitespaces) echo 'checked="checked"';?> value="1" name="remove_whitespaces"/> No quitar espacios multiples
	<input type="checkbox" <?php if (!$remove_enters) echo 'checked="checked"';?> value="1" name="remove_enters"/> No quitar saltos de linea
	<input type="checkbox" <?php if (!$remove_multiple_enters) echo 'checked="checked"';?> value="1" name="remove_multiple_enters"/> No quitar saltos de linea multiples
	<input type="submit" />
</form>
<textarea cols="100" rows="15"><?php echo $text2?></textarea>
</body>
</html>

