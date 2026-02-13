<?php
define('DEBUG', false);

ini_set('html_errors', false);
if (DEBUG) {
	error_reporting(-1);
	ini_set('display_errors', true);
} else {
	error_reporting(0);
	ini_set('display_errors', false);
}

set_error_handler(function ($errno, $errstr, $errfile = null, $errline = null) {
	throw new ErrorException($errstr, 0, $errno, $errfile, $errline);
});

// Initialize Parameters
$isPostRequest = $_SERVER['REQUEST_METHOD'] === 'POST';

// Call Key Cylinder Roles Mapper
$message = null;
if ($isPostRequest) {
	$sources = array();
	if (isset($_FILES['source'])) {
		$temp = tempnam('temp', '');
		unlink($temp);
		mkdir($temp);

		$source = $temp . '/_' . preg_replace('/[^ !#$%&\'()+,\\-.0-9;=@A-Z[\\]^_a-z{}~]+/', '', basename($_FILES['source']['name']));
		if ($_FILES['source']['error'] != UPLOAD_ERR_OK || !move_uploaded_file($_FILES['source']['tmp_name'], $source)) {
			http_response_code(400);
			$message = 'Beim Hochladen des Ist-Dokuments ist ein Fehler aufgetreten. Versuchen Sie es erneut.';
			break;
		}

		$destination = $temp . '/_' . preg_replace('/[^ !#$%&\'()+,\\-.0-9;=@A-Z[\\]^_a-z{}~]+/', '', basename($_FILES['destination']['name']));
		if ($_FILES['destination']['error'] != UPLOAD_ERR_OK || !move_uploaded_file($_FILES['destination']['tmp_name'], $destination)) {
			http_response_code(400);
			$message = 'Beim Hochladen des Planungsdokuments ist ein Fehler aufgetreten. Versuchen Sie es erneut.';
			break;
		}
	}

	$result = null;
	if (count($sources) === 0) {
		http_response_code(400);
		$message = 'Sie müssen eine Ist- und eine Planungsdatei zum Hochladen auswählen.';
	} else {
		$exit = false;
		try {
			$result = callKeyCylinderRolesMapper($source, $destination);
		} catch (Exception $e) {
			http_response_code(500);
			if (DEBUG) {
				header('Content-Type: text/plain; charset=UTF-8');
				echo $e;
				$exit = true;
			} else {
				$message = 'Das Vergleichen der Dokumente ist unerwartet fehlgeschlagen.';
			}
		} finally {
			deleteRecursively($temp);
		}
		if ($exit) {
			exit;
		}
	}
}

function callKeyCylinderRolesMapper($source, $destination) {
	$command = '/usr/bin/java -jar';
	$command .= ' ' . escapeshellarg(realpath('libraries/key-cylinder-roles-mapper-0.9.0-SNAPSHOT.jar'));
	$command .= ' ' . escapeshellarg(realpath($source));
	$command .= ' ' . escapeshellarg(realpath($destination));

	exec($command . ' 2>&1', $result, $result_code);
	if ($result_code !== 0) {
		throw new Exception(sprintf('Unexpected result code %d when executing: %s' . "\n" . '%s', $result_code, $command, implode("\n", $result)));
	}
	return $result;
}

function deleteRecursively($path) {
	if (is_file($path)) {
		return unlink($path);
	}

	if (is_dir($path)) {
		foreach (scandir($path) as $file) {
			if ($file !== '.' && $file !== '..') {
				deleteRecursively($path . '/' . $file);
			}
		}
		return rmdir($path);
	}

	return !file_exists($path);
}

// Content Type
$type = 'application/xhtml+xml';
if (stripos($_SERVER['HTTP_ACCEPT'], $type) === false) {
	$type = 'text/html';
}

// Header
header('Content-Type: ' . $type . '; charset=UTF-8');
echo '<?xml version="1.0" encoding="UTF-8" ?>';
?>
<!DOCTYPE html>
<html lang="de" xmlns="http://www.w3.org/1999/xhtml" xml:lang="de">
	<head>
		<link href="https://key-cylinder-roles-mapper.knickrehm.net/" rel="canonical" />
		<link href="get/style.css" rel="stylesheet" type="text/css" />
		<title>Key Cylinder Roles Mapper</title>
	</head>
	<body>
		<div class="content">
			<h1>Key Cylinder Roles Mapper</h1>
			<p class="lead">Der Key Cylinder Roles Mapper vergleicht CSV- und Excel-Dokumente mit Schlüssel-Schließzylinder-Berechtigungen.  liest Exporte der Software „CIP kommunal“ ein und bereitet sie in echter Tabellenform auf, um das Erstellen von Auswertungen zu erleichtern.</p>
			<p>CSV-Dateien müssen dem Exportformat von SimonVoss Locking System Management Basic entsprechen. Excel-Dokumente müssen einem eigenen Format (<a href="get/template.xlst">TODO: Vorlage</a>) entsprechen und erlauben die rollenbasierte Planung von Berechtigungen.</p>
			<p style="font-size: 80%;">Dieser Dienst wird ohne Gewähr bereitgestellt von <a href="https://lars-sh.de/" target="_blank">Lars Knickrehm</a>. Die im Formular angegebenen Daten werden nur für die Zeit der automatisierten Verarbeitung der Anfrage gespeichert und nach Bereitstellung des angeforderten Dokuments gelöscht.</p>

			<?php
			if ($result !== null) {
				echo '<h3>Ergebnisse</h3>';
				if (count($result) === 0) {
					echo '<ul><li style="font-style: italic;">Keine Unterschiede gefunden.</li></ul>'
				} else {
					echo '<ul><li>' . implode('</li><li>', array_map(function ($value) {
						return htmlspecialchars($value, ENT_QUOTES, 'UTF-8');
					}, $result)) . '</li></ul>';
				}
			}
			?>

			<h3>Dokumente aufbereiten</h3>
			<?php echo $message === null ? '' : '<p class="warning">' . htmlspecialchars($message, ENT_QUOTES, 'UTF-8') .'</p>'; ?>

			<form>
				<p>
					<label for="source">Ist hochladen*</label>
					<input accept=".csv,.xls,.xlsx" name="source" id="source" required="true" type="file" />
				</p>
				<p>
					<label for="destination">Planung hochladen*</label>
					<input accept=".csv,.xls,.xlsx" name="destination" id="destination" required="true" type="file" />
				</p>
				<p>
					<div></div>
					<input formenctype="multipart/form-data" formmethod="post" type="submit" value="Aufbereiten und herunterladen" />
				</p>
			</form>
		</div>
		<div class="footer">
			<span style="float: right;"><a href="https://lars-sh.de/impressum.html">Impressum</a></span>
			© <a href="https://lars-sh.de/" target="_blank">Lars Knickrehm</a>
		</div>
	</body>
</html>
