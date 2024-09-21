import datetime
import json
import os
import sys
import time

libPath = os.path.dirname(os.path.abspath(__file__)) + '/lib'
sys.path.append(libPath)
import boto3

songData = {}
minTimestamps = {}
table_name = 'tracktrace'
pathToSongs = 'data/'
jsonFiles = [jsonFile for jsonFile in os.listdir(pathToSongs) if jsonFile.endswith('.json')]

for jsonFile in jsonFiles:
  dataFile = open(pathToSongs + jsonFile)
  data = json.load(dataFile)
  dataFile.close()

  for songEntry in data:
    trackId = songEntry['spotify_track_uri']
    timestamp = datetime.datetime.strptime(songEntry['ts'], "%Y-%m-%dT%H:%M:%SZ").replace(tzinfo=datetime.timezone.utc).timestamp()
    title = songEntry['master_metadata_track_name']
    artist = songEntry['master_metadata_album_artist_name']

    if not trackId:
      continue

    if songEntry['reason_end'] == "trackdone" and songEntry['ms_played']  > 20_000:
      key = (title, artist)

      if trackId not in songData:
        songData[trackId] = {'songName': title, 'artistName': artist}

      if key not in minTimestamps:
        minTimestamps[key] = int(timestamp)
      else:
        minTimestamps[key] = min(minTimestamps[key], int(timestamp))

print("Writing", len(songData), "songs to table")

dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
table = dynamodb.Table(table_name)

for key, value in songData.items():
  print("Writing:", key)
  timestampKey = (value['songName'], value['artistName'])

  table.put_item(Item={
    "trackURI": key,
    "timestamp": minTimestamps[timestampKey],
    "artistName": value['artistName'],
    "trackName": value['songName']
    })

  time.sleep(0.05)

