import sys
import os
import glob
from google.protobuf import json_format
import keep_radius_pb2

def convert_pb_to_json(input_pb_path, output_json_path):
    bundle = keep_radius_pb2.BlastRadiusContainer()

    try:
        with open(input_pb_path, "rb") as pb_file:
            binary_data = pb_file.read()
    except Exception as e:
        print(f"Error reading file {input_pb_path}: {e}", file=sys.stderr)
        return False

    try:
        bundle.ParseFromString(binary_data)
    except Exception as e:
        print(f"Error parsing protobuf: {e}", file=sys.stderr)
        return False

    try:
        json_string = json_format.MessageToJson(
            bundle,
            including_default_value_fields=True,
            preserving_proto_field_name=True,
            indent=4
        )
        with open(output_json_path, "w", encoding="utf-8") as json_file:
            json_file.write(json_string)
        return True
    except Exception as e:
        print(f"Error writing JSON: {e}", file=sys.stderr)
        return False

if __name__ == "__main__":
    # Check if the specific path exists first
    default_pb = "composeApp/build/reports/r8/r8-config-analyzer-release.pb"
    input_pb = sys.argv[1] if len(sys.argv) > 1 else (default_pb if os.path.exists(default_pb) else None)

    if not input_pb:
        pb_files = glob.glob("tmp/r8analysis/*.pb")
        if not pb_files:
            print("Error: No .pb file found", file=sys.stderr)
            sys.exit(1)
        input_pb = sorted(pb_files)[-1] # Use the most recent one

    output_json = sys.argv[2] if len(sys.argv) > 2 else "tmp/r8analysis/keepruleradius.json"
    if not convert_pb_to_json(input_pb, output_json):
        sys.exit(1)
