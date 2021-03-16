# Barcode Generator

## Reference for the other teams

### How to generate a new barcode
In the Realtime Database, add a new key-value pair to the
'queued-barcodes' node.

If you want to generate a new barcode using the value '1234',
put a new child entry in 'queued-barcodes' with the value '1234'.
````json
"REALTIME DATABASE": {

	"queued-barcodes": {
		"0": "1234"
	}
}
````

**NOTE:** As the key is ignored anyway, use the 'push()' function of
Firebase to add a new value with a unique key.
````java
// In JAVA:
databaseReference.child("queued-barcodes").push().setValue("1234", null);
````

After you added the value to the Realtime Database, it will immediately
be processed by the barcode generator. This usually takes only a few
milliseconds. The result will be put in the 'generated-barcodes' node.
````json
"REALTIME DATABASE": {

	"generated-barcodes": {
		"1234": "barcodes/generatedbarcode.png"
	}
}
````

The path can then be used to navigate through the Storage database and
download the picture.
````json
"STORAGE DATABASE": {

	"barcodes": {
		[generatedbarcode.png]
	}
}
````

**NOTE:** Currently the barcodes are being stored as 'PNG' files. This is
subject to change, as the filetype will be 'SVG' in future releases.

## Setup
Simply clone this project, start including the 'key.json' and build the .jar file!

Please note that the 'key.json' file is not included on the GitHub repo,
since it contains confidential information. See the section below on how to
find the file on Firebase. 

### How to get 'key.json'
Use direct link, choose the 'HTLGKR-Testet' project and generate a new key:
https://console.firebase.google.com/project/_/settings/serviceaccounts/adminsdk

Or follow these steps:
1. Navigate to the Firebase console
2. Choose the 'HTLGKR-Testet' project
3. Click the cogwheel and choose 'Project settings'
4. Go to the tab 'Service accounts'
5. Click 'Firebase Admin SDK'
6. Click 'Generate new private key'
7. Put the generated JSON file in the project root
