import boto3
import json
import os
import sys

libPath = os.path.dirname(os.path.abspath(__file__)) + '/lib'
sys.path.append(libPath)
import spotipy

table_name = 'tracktrace'
unauthorizedRequestResponse = {
  'statusCode': 401,
  'body': json.dumps('Unauthorized Request')
}

def vaildQueryKey(presentedKey):
  return os.environ['expectedSecurityKey'] == presentedKey

def getAuthToken(dynamoTable):
  response = dynamoTable.get_item(Key={'trackId': 'auth-token'})

  return response['Item']['trackName']

def getSongFromTable(dynamoTable, trackId):
  response = dynamoTable.get_item(Key={'trackId': trackId})
  if 'Item' not in response:
    return None

  return response['Item']

def getCurrentTrackId(sp):
  response = sp.current_playback()
  if not response:
    return None

  return response['item']['uri']

def lambda_handler(event, context):
  if 'queryStringParameters' not in event:
    return unauthorizedRequestResponse
  if 'key' not in event['queryStringParameters']:
    return unauthorizedRequestResponse

  presentedQueryKey = event['queryStringParameters']['key']

  if not vaildQueryKey(presentedQueryKey):
    return unauthorizedRequestResponse

  dynamodb = boto3.resource('dynamodb', region_name='us-east-1')
  table = dynamodb.Table(table_name)
  sp = spotipy.Spotify(auth=getAuthToken(table))

  currentTrackId = getCurrentTrackId(sp)

  if not currentTrackId:
    return {
      'statusCode': 200,
      'body': json.dumps('No track playing'),
      'headers': {
        'Content-Type': 'application/json'
      }
    }

  currentlyPlayingFromTable = getSongFromTable(table, currentTrackId)

  if not currentlyPlayingFromTable:
    return {
      'statusCode': 200,
      'body': json.dumps('Song not in table'),
      'headers': {
        'Content-Type': 'application/json'
      }
    }

  responseData = {
    'timestamp': str(currentlyPlayingFromTable['timestamp']),
    'trackName': str(currentlyPlayingFromTable['trackName'])
  }

  return {
    'statusCode': 200,
    'body': json.dumps(responseData),
    'headers': {
      'Content-Type': 'application/json'
    }
  }
