######################################################
## An applet to securely grant acces to user's hard ##
## drive from a webpage (e.g. javascript app)       ##
######################################################

You can get the compiled version directly from the "release" folder of this project.

Below is a simple code snippet on how to use it.
Important notes:
 * the jar must be signed for the browser to propose additional privileges to the user (priviledge to access the file system). You can create necessary files to sign the jar by running "keytool -genkey -alias applet-key -keystore applet-keystore".

...
    <script type="text/javascript">
     function writeHello(who, where) {
            var japplet = document.getElementById("japplet");
            japplet.writeFile("Hello "+who, where);
            alert("File should have been written.");
        }
    </script>
...
    <applet id="japplet" code="com/heeere/fileaccessapplet/FacadeApplet.class" archive="target/FileAccessApplet-1.0.jar"  width="2" height="2" mayscript="true"></applet>
...
