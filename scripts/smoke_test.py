"""개발 모드 전용 HTTP 통합 검증. 임시 회원을 생성/삭제합니다 (Python 3 표준 라이브러리)."""
import base64, datetime, html, http.cookiejar, json, re, struct, sys, urllib.error, urllib.parse, urllib.request, uuid, zlib

BASE = (sys.argv[1] if len(sys.argv) > 1 else 'http://localhost:18080/enjoytrip').rstrip('/')
PASS = 'smoke-password-12345'

class Browser:
    def __init__(self):
        self.jar = http.cookiejar.CookieJar()
        self.client = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(self.jar))
        self.csrf = ''

    def request(self, path, data=None, content_type=None):
        if isinstance(data, dict):
            data = urllib.parse.urlencode({'csrf': self.csrf, **data}).encode()
        req = urllib.request.Request(path if path.startswith('http') else BASE + path, data=data)
        if content_type:
            req.add_header('Content-Type', content_type)
        try:
            response = self.client.open(req, timeout=20)
        except urllib.error.HTTPError as error:
            response = error
        body = response.read()
        if not response.headers.get('Content-Type', '').startswith('image/'):
            text = body.decode('utf-8')
            token = re.search(r'name="csrf" value="([^"]+)"', text)
            if token:
                self.csrf = html.unescape(token.group(1))
            return response.status, text, response.url
        return response.status, body, response.url

    def get(self, path, expected=200):
        status, body, url = self.request(path)
        assert status == expected, (path, status, body[-300:])
        return body, url

    def post(self, path, data, expected=200):
        status, body, url = self.request(path, data)
        assert status == expected, (path, status, body[-300:])
        return body, url


def image_bytes():
    def chunk(name, value):
        return struct.pack('!I', len(value)) + name + value + struct.pack('!I', zlib.crc32(name + value))
    return b'\x89PNG\r\n\x1a\n' + chunk(b'IHDR', struct.pack('!2I5B', 2, 2, 8, 2, 0, 0, 0)) + chunk(b'IDAT', zlib.compress((b'\x00' + b'\x80\xb0\x80' * 2) * 2)) + chunk(b'IEND', b'')


def main():
    a, b = Browser(), Browser()
    health, _ = a.get('/health')
    assert json.loads(health)['demo'], '개발 모드에서만 실행할 수 있습니다.'
    home, _ = a.get('/')
    assert '어디로 떠나고' in home and '개발 모드' in home
    a.post('/auth/register', {'email': 'blocked@example.com', 'name': '차단', 'password': PASS, 'csrf': 'wrong'}, 403)
    a.get('/auth/register')
    # 관리자 기능 검증 시 APP_ADMIN_EMAILS에 smoke@example.com을 설정한다.
    a.post('/auth/register', {'email': 'smoke@example.com', 'name': '여행자<확인>', 'password': PASS})
    body, _ = a.get('/members/me')
    assert '여행자&lt;확인&gt;' in body
    a.post('/members/update', {'email': 'smoke@example.com', 'name': '수정한 회원', 'password': 'incorrect', 'newPassword': ''}, 400)
    a.post('/members/update', {'email': 'smoke@example.com', 'name': '수정한 회원', 'password': PASS, 'newPassword': ''})
    body, _ = a.get('/attractions?area=11&type=12')
    assert '[샘플]' in body
    attraction = re.search(r'/attractions/detail\?id=(\d+)', body).group(1)
    a.get('/attractions?area=11&type=12')
    detail, _ = a.get('/attractions/detail?id=' + attraction)
    assert '여행 일정' in detail
    today = datetime.date.today()
    _, url = a.post('/plans/create', {'title': '검증용 서울 여행', 'start': str(today), 'end': str(today + datetime.timedelta(days=1)), 'budget': '100000', 'description': '일정 메모'})
    plan = urllib.parse.parse_qs(urllib.parse.urlparse(url).query)['id'][0]
    body, _ = a.post('/plans/add', {'id': plan, 'contentId': attraction, 'day': '1', 'cost': '10000', 'notes': '메모'})
    assert '검증용 서울 여행' in body
    a.post('/plans/add', {'id': plan, 'contentId': attraction, 'day': '2', 'cost': '0', 'notes': '다음 날'})
    body, _ = a.get('/plans/detail?id=' + plan)
    stops = re.findall(r'data-stop-id="(\d+)"', body)
    assert len(stops) == 2
    a.post('/plans/reorder', {'id': plan, 'order': ','.join(reversed(stops))})
    b.get('/auth/register')
    b.post('/auth/register', {'email': 'other-smoke@example.com', 'name': '다른 회원', 'password': PASS})
    b.get('/plans/detail?id=' + plan, 404)
    b.post('/plans/delete', {'id': plan}, 404)
    body, url = a.post('/community/create', {'board': 'share', 'title': '검증 게시글', 'body': '<script>alert(1)</script>'})
    post = urllib.parse.parse_qs(urllib.parse.urlparse(url).query)['id'][0]
    assert '&lt;script&gt;' in body and '<script>alert(1)</script>' not in body
    b.get('/community/edit?id=' + post, 403)
    b.post('/community/delete', {'id': post}, 403)
    b.post('/community/create', {'board': 'notice', 'title': '금지된 공지', 'body': '공지'}, 403)
    a.get('/community/edit?id=' + post)
    a.post('/community/update', {'id': post, 'title': '수정 게시글', 'body': '수정 내용'})
    notices, _ = a.get('/community?board=notice')
    if '글쓰기' in notices:
        body, _ = a.post('/community/create', {'board': 'notice', 'title': '검증 공지', 'body': '공지 내용'})
        assert '검증 공지' in body
    a.get('/places')
    boundary = 'EnjoyTrip' + uuid.uuid4().hex
    values = {'csrf': a.csrf, 'title': '사진 여행', 'address': '서울', 'type': '관광지', 'visitedOn': str(today), 'description': '업로드 검증', 'latitude': '37.57', 'longitude': '126.97'}
    multipart = b''.join((f'--{boundary}\r\nContent-Disposition: form-data; name="{key}"\r\n\r\n{value}\r\n'.encode() for key, value in values.items()))
    multipart += f'--{boundary}\r\nContent-Disposition: form-data; name="photo"; filename="sample.png"\r\nContent-Type: image/png\r\n\r\n'.encode() + image_bytes() + f'\r\n--{boundary}--\r\n'.encode()
    status, body, _ = a.request('/places/create', multipart, 'multipart/form-data; boundary=' + boundary)
    assert status == 200 and '사진 여행' in body, ('upload', status, body[-300:])
    photo = re.search(r'/photos/([a-f0-9-]+\.jpg)', body).group(1)
    image, _ = a.get('/photos/' + photo)
    assert image[:2] == b'\xff\xd8'
    a.get('/auth/forgot')
    body, _ = a.post('/auth/forgot', {'email': 'smoke@example.com'})
    link = html.unescape(re.search(r'href="(http[^"]+/auth/reset\?token=[^"]+)"', body).group(1))
    token = urllib.parse.parse_qs(urllib.parse.urlparse(link).query)['token'][0]
    a.get(link)
    a.post('/auth/reset', {'token': token, 'password': 'changed-password-12345'})
    a.post('/auth/reset', {'token': token, 'password': 'another-password-12345'}, 400)
    a.get('/auth/login')
    a.post('/auth/login', {'email': 'smoke@example.com', 'password': PASS}, 400)
    a.post('/auth/login', {'email': 'smoke@example.com', 'password': 'changed-password-12345'})
    a.get('/members/me')
    a.post('/members/delete', {'password': 'changed-password-12345'})
    a.get('/photos/' + photo, 404)
    b.get('/members/me')
    b.post('/members/delete', {'password': PASS})
    a.get('/news')
    health, _ = a.get('/health')
    print(json.dumps({'result': 'PASS', 'checks': ['한글 JSP', '회원 CRUD', 'CSRF', '로그인', '개인 일정·소유권', '게시판 CRUD·권한·XSS', '공지 작성 권한', '사진 업로드·정리', '일회용 비밀번호 재설정'], 'health': json.loads(health)}, ensure_ascii=False))

if __name__ == '__main__':
    main()
