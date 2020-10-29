""" This Module is used to run basic Black Box Testing for the TFTP Project """

import subprocess
import time
import hashlib
import os
import random
import uuid
import tempfile


# Setting filepaths
# TODO Replace with tempfolder and tempfiles
source_dir_path = os.path.join(os.path.dirname(os.path.realpath(__file__)), "..")
app_location = os.path.join(source_dir_path, "target/tftp.jar")
certificates_folder = os.path.join(source_dir_path, "certificates/localhost")

# Configure Keystore
keystore_location = f'{certificates_folder}/KeyStore.jks'
keystore_password = "tester1234"
java_options = f'-Djavax.net.ssl.trustStore={keystore_location} -Djavax.net.ssl.trustStorePassword={keystore_password}'

PORT_MIN = 33333
PORT_MAX = 44444

# Configure App Arguments


# TODO LIST
# Create a folder where files can be store
import tempfile

# Echo a file to the folder
# Share folder with the Server
# List folder contents
# Copy a file from the server
# Verify the SHAs of the files

# Globals


class BaseTest:

    def __init__(self):
        self.server_port = self.getPort()
        # Create temp folder
        self.temp_folder = tempfile.TemporaryDirectory(dir="/tmp")
        self.hosted_folder = self.temp_folder.name

        self.requested_filename = self.getName()
        self.output_filename = self.getName()
        self.output_file_location = os.path.join(self.hosted_folder, self.output_filename)
        self.requested_file_location = os.path.join(self.hosted_folder, self.requested_filename)

        self.server_process = None

    def startClient(self):
        exec_command = f'java {java_options} -jar {app_location} client copy localhost {str(self.server_port)} -o ' + \
                       f'{self.output_file_location} {self.requested_filename}'

        print(exec_command)
        command_output = subprocess.Popen(exec_command, shell=True)
        time.sleep(5)
        print("Command Output")
        print(command_output)

    def startServer(self):
        self.createFileForServer()
        exec_command = f'java {java_options} -jar {app_location} server --key-store {keystore_location} ' + \
                       f'--key-store-password {keystore_password} {str(self.server_port)} {self.hosted_folder}'
        print(exec_command)
        self.server_process = subprocess.Popen(exec_command, shell=True)
        time.sleep(2)

    def stopServer(self):
        self.server_process.terminate()

    def checkFileHashes(self):
        requested_file_location = os.path.join(self.hosted_folder, self.requested_filename)
        output_file_location = os.path.join(self.hosted_folder, self.output_filename)
        requested_file_hash = self.getFileHash(requested_file_location)
        output_file_hash = self.getFileHash(output_file_location)

        print("request filehash")
        print(requested_file_hash)

        print("output filehash")
        print(output_file_hash)

        assert (requested_file_hash == output_file_hash)

    #TODO Belo can probably be static
    def getPort(self):
        return random.randint(PORT_MIN, PORT_MAX)

    def getName(self):
        return str(uuid.uuid4())

    def getRandomFileContents(self, length):
        return os.urandom(length)

    def getFileHash(self, file_location):
        # NOTE: Will only work on small files but this is testing so not worth complexity of chunking yet.
        with open(file_location,"rb") as f:
            bytes = f.read()
            readable_hash = hashlib.sha256(bytes).hexdigest()
        return readable_hash

    def createFileForServer(self):
        file_contents = self.getRandomFileContents(1024)
        file = open(os.path.join(self.hosted_folder, self.requested_filename), 'wb')
        file.write(file_contents)
        file.close()

    def cleanup(self):
        self.stopServer()
        self.temp_folder.cleanup()

class BasicTest(BaseTest):
    def run_test(self):
        self.startServer()
        self.startClient()

        self.checkFileHashes()
        # Cleanup
        self.cleanup()

# TODO Temporary Blackbox Test ---- TODO replace with Base_Test Object and expand on testing
bt = BasicTest()
bt.run_test()