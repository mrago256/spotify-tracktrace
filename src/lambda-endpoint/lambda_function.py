import boto3
import json
import os
import sys

lib_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'lib')
sys.path.append(lib_path)
import spotipy

TABLE_NAME = 'tracktrace'
UNAUTHORIZED_RESPONSE = {
  'statusCode': 401,
  'body': json.dumps('Unauthorized Request')
}

def is_valid_query_key(presented_key):
  return os.environ['expectedSecurityKey'] == presented_key

def get_auth_token(dynamo_table):
  response = dynamo_table.get_item(
    Key={
      'trackName': 'auth-token',
      'artistName': 'auth'
    }
  )

  return response['Item']['token']

def get_song_from_table(dynamo_table, current_track):
  track_name = current_track.get('name')
  artist_name = current_track['artists'][0].get('name')

  response = dynamo_table.get_item(
    Key={
      'trackName': track_name,
      'artistName': artist_name
    }
  )

  return response.get('Item')

def get_current_track(spotify_client):
  response = spotify_client.current_playback()
  return response.get('item') if response else None

def lambda_handler(event, context):
  query_params = event.get('queryStringParameters', {})
  presented_query_key = query_params.get('key')

  if not presented_query_key or not is_valid_query_key(presented_query_key):
    return UNAUTHORIZED_RESPONSE

  dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
  table = dynamodb.Table(TABLE_NAME)
  sp = spotipy.Spotify(auth=get_auth_token(table))

  current_track = get_current_track(sp)

  if not current_track:
    return {
      'statusCode': 200,
      'body': json.dumps('No track playing'),
      'headers': {
        'Content-Type': 'application/json'
      }
    }

  song_from_table = get_song_from_table(table, current_track)

  if not song_from_table:
    return {
      'statusCode': 200,
      'body': json.dumps('Song not in table'),
      'headers': {
        'Content-Type': 'application/json'
      }
    }

  response_data = {
    'timestamp': str(song_from_table['timestamp']),
    'trackName': str(song_from_table['trackName'])
  }

  return {
    'statusCode': 200,
    'body': json.dumps(response_data),
    'headers': {
      'Content-Type': 'application/json'
    }
  }
