import datetime
import json
import os
import sys
import time

libPath = os.path.dirname(os.path.abspath(__file__)) + '/lib'
sys.path.append(libPath)
import boto3

songData = {}
table_name = 'tracktrace'
dataFile = open("../../final-sorted.json")
data = json.load(dataFile)

# TODO: read multiple files at a time since they come that way
# TODO: make option to create table automatically, like pass in param

for songEntry in data:
  trackId = songEntry['spotify_track_uri']
  timestamp = datetime.datetime.strptime(songEntry['ts'], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=datetime.timezone.utc).timestamp()
  title = songEntry['master_metadata_track_name']

  if not trackId:
    continue

  if songEntry['reason_end'] == "trackdone" and songEntry['ms_played']  > 20_000:
    if trackId in songData:
      if timestamp < songData[trackId]['timestamp']:
        songData[trackId]['timestamp'] = timestamp
    else:
      songData[trackId] = {'songName': title, 'timestamp': int(timestamp)}

dataFile.close()

print("Writing", len(songData), "songs to table")

dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
table = dynamodb.Table(table_name)

for key, value in songData.items():
  print("Writing:", key)
  table.put_item(Item={
    "trackId": key,
    "timestamp": value['timestamp'],
    "trackName": value['songName']
    })

  time.sleep(0.05)

