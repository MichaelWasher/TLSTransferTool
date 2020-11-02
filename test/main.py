""" This Module is used to run basic Black Box Testing for the TFTP Project """

import subprocess
import time
import hashlib
import os
import random
import uuid
import tempfile
import unittest

# TODO Add docstrings to module
# TODO Make tests to extend Pytest

# Setting filepaths
source_dir_path = os.path.join(os.path.dirname(os.path.realpath(__file__)), "..")
app_location = os.path.join(source_dir_path, "target/tftp.jar")
certificates_folder = os.path.join(source_dir_path, "certificates/localhost")

# Configure Keystore
keystore_location = f'{certificates_folder}/KeyStore.jks'
keystore_password = "tester1234"
java_options = f'-Djavax.net.ssl.trustStore={keystore_location} -Djavax.net.ssl.trustStorePassword={keystore_password}'

PORT_MIN = 33333
PORT_MAX = 44444


class BaseTest(unittest.TestCase):

    def __init__(self, name):
        super(BaseTest, self).__init__(name)
        self.server_port = self.getPort()
        # Create temp folder
        self.temp_folder = tempfile.TemporaryDirectory(dir="/tmp")
        self.hosted_folder = self.temp_folder.name
        self.hosted_folder_sub_filenames = [self.getName() for x in range(10)]
        self.requested_filename = self.hosted_folder_sub_filenames[0]
        self.output_filename = self.getName()

        self.output_file_location = os.path.join(self.hosted_folder, self.output_filename)
        self.requested_file_location = os.path.join(self.hosted_folder, self.requested_filename)

        self.server_process = None

    def setUp(self):
        self.populateFolder(self.hosted_folder, self.hosted_folder_sub_filenames)
        self.startServer()

    def tearDown(self):
        self.stopServer()
        self.temp_folder.cleanup()

    def clientGetRequest(self):
        exec_command = f'java {java_options} -jar {app_location} client copy localhost {str(self.server_port)} -o ' + \
                       f'{self.output_file_location} {self.requested_filename}'

        print(exec_command)
        command_output = subprocess.check_output(exec_command, shell=True)
        print("Command Output")
        print(command_output)
        return command_output

    def clientListRequest(self):
        exec_command = f'java {java_options} -jar {app_location} client list localhost {str(self.server_port)}'

        print(exec_command)
        command_output = subprocess.check_output(exec_command, shell=True)
        command_output = command_output.decode("utf-8")

        print("Command Output")
        print(command_output)
        return command_output

    def startServer(self):
        exec_command = f'java {java_options} -jar {app_location} server --key-store {keystore_location} ' + \
                       f'--key-store-password {keystore_password} {str(self.server_port)} {self.hosted_folder}'
        print(exec_command)
        self.server_process = subprocess.Popen(exec_command, shell=True)
        # TODO Replace with searching command output
        time.sleep(1)

    def stopServer(self):
        self.server_process.terminate()

    @staticmethod
    def getPort():
        return random.randint(PORT_MIN, PORT_MAX)

    @staticmethod
    def populateFolder(folder, filenames):
        for filename in filenames:
            file_location = os.path.join(folder, filename)
            BaseTest.createRandomFile(file_location, 1024)

    @staticmethod
    def getName():
        return str(uuid.uuid4())

    @staticmethod
    def getRandomFileContents(length):
        return os.urandom(length)

    @staticmethod
    def getFileHash(file_location):
        # NOTE: Will only work on small files but this is testing so not worth complexity of chunking yet.
        with open(file_location, "rb") as f:
            file_bytes = f.read()
            readable_hash = hashlib.sha256(file_bytes).hexdigest()
        return readable_hash

    @staticmethod
    def createRandomFile(file_location, file_size):
        file_contents = BaseTest.getRandomFileContents(file_size)
        file = open(file_location, 'wb')
        file.write(file_contents)
        file.close()


class ClientGetSmokeTest(BaseTest):
    def __init__(self, name):
        super(ClientGetSmokeTest, self).__init__(name)

    def checkFileHashes(self):
        requested_file_location = os.path.join(self.hosted_folder, self.requested_filename)
        output_file_location = os.path.join(self.hosted_folder, self.output_filename)
        requested_file_hash = self.getFileHash(requested_file_location)
        output_file_hash = self.getFileHash(output_file_location)

        # Assert that both file hashes are equal
        assert (requested_file_hash == output_file_hash)

    def testGetSmokeTest(self):
        self.clientGetRequest()
        self.checkFileHashes()


class ClientListSmokeTest(BaseTest):
    def __init__(self, name):
        super(ClientListSmokeTest, self).__init__(name)

    def checkListOutput(self, command_output):
        for filename in self.hosted_folder_sub_filenames:
            if command_output.find(filename) == -1:
                return False

            command_output = command_output.replace(filename, "")

        command_output = command_output.strip()

        assert command_output == ""

    def testClientListSuccess(self):

        command_output = self.clientListRequest()
        res = self.checkListOutput(command_output)


    # def testClientListBlankFolder(self):
    #     pass
    # TODO List Empty folder
    # TODO Copy Missing File
    # TODO

if __name__ == '__main__':
    unittest.main()