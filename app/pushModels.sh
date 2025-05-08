#!/bin/bash
#
# SPDX-FileCopyrightText: Copyright 2024-2025 Arm Limited and/or its affiliates <open-source-office@arm.com>
#
# SPDX-License-Identifier: Apache-2.0
#

local_models_dir=$1
device_dir=$2

# Create directory on the device if doesn't exist
adb shell mkdir -p "$device_dir/"

# Iterate over all .bin model in local folder
# Please Update if other model types are needed
for file in "$local_models_dir"/*; do

  echo " Checking $(basename "$file") "

  local_file_hash="$(md5sum "$file" | awk '{print $1;}')"
  device_file_hash="$(adb shell md5sum "$device_dir"/"$(basename "$file")" | awk '{print $1;}')"

  if [ "$local_file_hash" != "$device_file_hash" ]; then

    echo "File '$(basename "$file")' is missing on device, adb pushing to device"
    adb push "$file" "$device_dir/"

  fi
done
