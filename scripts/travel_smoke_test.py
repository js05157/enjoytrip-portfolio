"""여행 도구 HTTP 읽기 전용 검증. --fixtures는 CI의 fixture_api.py를 사용하는 서버에서만 지정한다."""
import datetime
import json
import sys
import urllib.request
import urllib.error

base = (sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:8080/enjoytrip').rstrip('/')
fixtures = '--fixtures' in sys.argv


def get(path, expected=200):
    try:
        response = urllib.request.urlopen(base+path, timeout=30)
    except urllib.error.HTTPError as error:
        response = error
    with response:
        body = response.read().decode('utf-8')
        assert response.status == expected, (path, response.status)
        return body


weather = get('/travel-info')
assert '여행 날씨와 해 뜨는 시간' in weather and '일출' in weather and '일몰' in weather
assert '<tbody>' in weather and 'Open-Meteo' in weather
if fixtures:
    assert '06:30' in weather and '18:00' in weather
today = datetime.datetime.now(datetime.timezone(datetime.timedelta(hours=9))).date()
get('/travel-info?latitude=NaN&longitude=127', 400)
get('/travel-info?start='+str(today)+'&end='+str(today-datetime.timedelta(days=1)), 400)
photos = get('/travel-photos?area=11')
assert '사진으로 고르는 다음 여행' in photos
chargers = get('/chargers?region=11')
assert '전기차 충전소 찾기' in chargers
if fixtures:
    assert '검증용 충전소' in chargers and '충전 대기' in chargers and 'data-lat=' in chargers
get('/chargers?region=wrong', 400)
health = json.loads(get('/health'))
assert health['database'] == 'UP'
print(json.dumps({'result': 'PASS', 'checks': ['기간별 날씨 JSP', '일출·일몰 KST', '날짜·좌표 검증', '사진 갤러리 JSP', '충전소 JSP·입력 검증'], 'fixtures': fixtures, 'health': health}, ensure_ascii=False))
