<?php
// summary
//		Test file to handle image uploads (remove the image size check to upload non-images)
//
//		This file handles both Flash and HTML uploads
//
//		NOTE: This is obviously a PHP file, and thus you need PHP running for this to work
//		NOTE: Directories must have write permissions
//		NOTE: This code uses the GD library (to get image sizes), that sometimes is not pre-installed in a 
//				standard PHP build. 
//
require("cLOG.php");
function findTempDirectory()
  {
    if(isset($_ENV["TMP"]) && is_writable($_ENV["TMP"])) return $_ENV["TMP"];
    elseif( is_writable(ini_get('upload_tmp_dir'))) return ini_get('upload_tmp_dir');
    elseif(isset($_ENV["TEMP"]) && is_writable($_ENV["TEMP"])) return $_ENV["TEMP"];
    elseif(is_writable("/tmp")) return "/tmp";
    elseif(is_writable("/windows/temp")) return "/windows/temp";
    elseif(is_writable("/winnt/temp")) return "/winnt/temp";
    else return null;
  }
function trace($txt){
	//creating a text file that we can log to
	// this is helpful on a remote server if you don't
	//have access to the log files
	//
	//echo($txt."<br/>");
	$log = new cLOG("../resources/upload.txt", false);
	//$log->clear();
	$log->write($txt);
}
function getImageType($filename){
	return strtolower(substr(strrchr($filename,"."),1));
}
trace("---------------------------------------------------------");
trace("TmpDir:".findTempDirectory());
//
//
//	EDIT ME: According to your local directory structure.
// 	NOTE: Folders must have write permissions
//
$upload_path = "../resources/"; 	// where image will be uploaded, relative to this file
$download_path = "../resources/";	// same folder as above, but relative to the HTML file

//
// 	NOTE: maintain this path for JSON services
//
require("../../../dojo/tests/resources/JSON.php");
$json = new Services_JSON();


if( isset($_FILES['Filedata'])){
	//
	// If the data passed has 'Filedata', then it's Flash. That's the default fieldname used.
	//
	trace("returnFlashdata.... ");
	$returnFlashdata = true;
	$m = move_uploaded_file($_FILES['Filedata']['tmp_name'],  $upload_path . $_FILES['Filedata']['name']);
	trace("moved:" . $m);
	$file = $upload_path . $_FILES['Filedata']['name'];
	list($width, $height) = getimagesize($file);
	$type = getImageType($file);
	trace("file: " . $file ."  ".$type." ".$width);
	// 		Flash gets a string back:
	$data ='file='.$file.',width='.$width.',height='.$height.',type='.$type;
	if($returnFlashdata){
		trace("returnFlashdata");
		echo($data);
		return $data;
	}

}elseif( isset($_FILES['uploadedfile']) ){
	//
	// 	If the data passed has 'uploadedfile', then it's HTML. 
	//	There may be better ways to check this, but this is just a test file.$returnFlashdata = false;
	//
	$m = move_uploaded_file($_FILES['uploadedfile']['tmp_name'],  $upload_path . $_FILES['uploadedfile']['name']);
	trace("moved:".$m);
	trace("Temp:".$_FILES['uploadedfile']['tmp_name']);
	$file = $upload_path . $_FILES['uploadedfile']['name'];
	$type = getImageType($file);
	list($width, $height) = getimagesize($file);
	trace("file: " . $file );
	$ar = array(
		'file' => $file,
		'width' => $width,
		'height' => $height,
		'type'=> $type
	);

}elseif( isset($_FILES['uploadedfile0']) ){
	//
	//	Multiple files have been passed from HTML
	//
	$cnt = 0;
	$ar = array();
	while(isset($_FILES['uploadedfile'.$cnt])){
		$moved = move_uploaded_file($_FILES['uploadedfile'.$cnt]['tmp_name'],  $upload_path . $_FILES['uploadedfile'.$cnt]['name']);
		if($moved){
			$file = $upload_path . $_FILES['uploadedfile'.$cnt]['name'];
			$type = getImageType($file);
			list($width, $height) = getimagesize($file);
			trace("file: " . $file );
			$ar[] = array(
				'file' => $file,
				'width' => $width,
				'height' => $height,
				'type'=> $type
			);
		}
		$cnt++;
	}
	
}else{
	//
	//	deleting files
	//
	trace("DELETING FILES" . $_GET['rmFiles']);
	$rmFiles = explode(";", $_GET['rmFiles']);
	foreach($rmFiles as $f){
		if($f && file_exists($f)){
			trace("deleted:" . $f. ":" .unlink($f));
		}
	}
	return;
}

//HTML gets a json array back:
$data = $json->encode($ar);
trace($data);
// in a text field:
?>
<textarea><?php print $data; ?></textarea>