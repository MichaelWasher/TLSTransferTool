""" This Module is used to run basic Black Box Testing for the TFTP Project """

import subprocess
import time
import hashlib
import os

# Setting filepaths
source_dir_path = os.path.join(os.path.dirname(os.path.realpath(__file__)), "..")
app_location = os.path.join(source_dir_path, "target/tftp.jar")
certificates_folder = os.path.join(source_dir_path, "certificates/localhost")

# Configure Keystore
keystore_location = f'{certificates_folder}/KeyStore.jks'
keystore_password = "tester1234"
java_options = f'-Djavax.net.ssl.trustStore={keystore_location} -Djavax.net.ssl.trustStorePassword={keystore_password}'

# Configure App Arguments
server_port = 44562 # TODO Random Port
requested_filename = "README.md"
output_filename = 'output_filename.md'
hosted_folder = source_dir_path

# TODO LIST
# Create a folder where files can be store
# Echo a file to the folder
# Share folder with the Server
# List folder contents
# Copy a file from the server
# Verify the SHAs of the files

# Globals
server_process = None

def startClient():
    exec_command = f'java {java_options} -jar {app_location} client copy localhost {str(server_port)} -o ' + \
                   f'{output_filename} {requested_filename}'
    print(exec_command)
    command_output = subprocess.Popen(exec_command, shell=True)
    time.sleep(5)
    print("Command Output")
    print(command_output)

def startServer():
    exec_command = f'java {java_options} -jar {app_location} server --key-store {keystore_location} ' + \
                   f'--key-store-password {keystore_password} {str(server_port)} {hosted_folder}'
    print(exec_command)
    globals()['server_process'] = subprocess.Popen(exec_command, shell=True)
    time.sleep(2)

def stopServer():
    server_process.terminate()

def checkFileHashes():
    requested_file_location = os.path.join(hosted_folder, requested_filename)
    output_file_location = os.path.join(os.getcwd(), output_filename)
    requested_file_hash = getFileHash(requested_file_location)
    output_file_hash = getFileHash(output_file_location)

    print("request filehash")
    print(requested_file_hash)

    print("output filehash")
    print(output_file_hash)

    assert (requested_file_hash == output_file_hash)

def getFileHash(file_location):
    # NOTE: Will only work on small files but this is testing so not worth complexity of chunking yet.
    with open(file_location,"rb") as f:
        bytes = f.read()
        readable_hash = hashlib.sha256(bytes).hexdigest()
    return readable_hash


# TODO Temporary Blackbox Test ---- TODO replace with Base_Test Object and expand on testing
startServer()
startClient()
stopServer()
checkFileHashes()
