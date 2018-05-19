
import json

build_directory = "../build/outputs/apk/release/"
apk_json = build_directory + "output.json"


apk=open().read(apk_json)

apk_meta = json.loads(apk)

