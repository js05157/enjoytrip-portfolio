"""CI 전용 외부 API 응답 서버. localhost에만 바인딩하며 실제 인증키/외부 통신은 사용하지 않는다."""
import datetime
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse, parse_qs


class Handler(BaseHTTPRequestHandler):
    def log_message(self, *_):
        pass

    def do_GET(self):
        url = urlparse(self.path)
        params = parse_qs(url.query)
        if url.path == '/forecast':
            start = datetime.date.fromisoformat(params['start_date'][0])
            end = datetime.date.fromisoformat(params['end_date'][0])
            dates = [str(start + datetime.timedelta(days=i)) for i in range((end-start).days+1)]
            value = {'daily': {'time': dates, 'weather_code': [0]*len(dates),
                              'temperature_2m_min': [10]*len(dates), 'temperature_2m_max': [20]*len(dates),
                              'precipitation_probability_max': [0]*len(dates),
                              'sunrise': [d+'T06:30' for d in dates], 'sunset': [d+'T18:00' for d in dates]}}
        elif url.path == '/getChargerInfo':
            value = {'resultCode': '00', 'totalCount': 1, 'items': {'item': [{
                'statId': 'CI01', 'statNm': '검증용 충전소', 'chgerId': '01', 'chgerType': '04',
                'addr': '검증용 주소', 'lat': '37.57', 'lng': '126.98', 'stat': '2',
                'statUpdDt': datetime.datetime.now().strftime('%Y%m%d%H%M%S'), 'useTime': '24시간',
                'busiNm': '검증기관', 'busiCall': '', 'output': '100', 'parkingFree': 'Y'}]}}
        else:
            self.send_error(404)
            return
        data = json.dumps(value, ensure_ascii=False).encode('utf-8')
        self.send_response(200)
        self.send_header('Content-Type', 'application/json;charset=UTF-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == '__main__':
    ThreadingHTTPServer(('127.0.0.1', 18190), Handler).serve_forever()
