import subprocess
import socket
import threading

OPS = {
    'launch': '',
    'play_pause': '/playpause',
    'next': '/next',
    'prev': '/prev',
    'stop': '/command:stop',
    'random': '/runcmd=Playback/Random',
    'vol_up': '/command:up',
    'vol_down': '/command:down',
    'default': '/runcmd=Playback/Order/Default',
    'repeat_playlist': '/runcmd=Playback/Order/Repeat (playlist)',
    'repeat_track': '/runcmd=Playback/Order/Repeat (track)',
    'order_random': '/runcmd=Playback/Order/Random',
    'shuffle_tracks': '/runcmd=Playback/Order/Shuffle (tracks)',
    'shuffle_albums': '/runcmd=Playback/Order/Shuffle (albums)',
    'shuffle_folders': '/runcmd=Playback/Order/Shuffle (folders)',
    'disc': '',
    'ack': ''
}

OP_NAMES = list(OPS.keys())


def check_playing_track(sock, interval):
    stopped = threading.Event()

    def loop():
        curr_track = ''
        while not stopped.wait(interval):
            with open(r'C:\Users\RonMad\Documents\foobar2000_now_playing\now_playing.txt',
                      encoding='utf-8-sig') as np_file:
                track = np_file.readline()
                if track != curr_track:
                    send(sock, track)
                    curr_track = track
                    

    threading.Thread(target=loop).start()
    return stopped.set


def send(sock, msg):
    sock.send(bytes(msg + '\r\n', 'utf-8'))


def foobar_action(sock, op):
    args = ['cmd', '/c', 'start', 'foobar2000.exe']
    if OPS[op]:
        args.append(OPS[op])
    subprocess.run(args, cwd=r'C:\Program Files (x86)\foobar2000')    


def main():
    with socket.socket() as sock:
        sock.bind(('', 5050))
        sock.listen(1)
        print('Listening on ' + socket.gethostbyname(socket.gethostname()))
        while True:
            conn, addr = sock.accept()
            with conn:
                send(conn, str(OP_NAMES.index('ack')))
                print('Connection established')
                stop_track_check = check_playing_track(conn, 0.1)
                while True:
                    try:
                        data = conn.recv(1024)
                        if not data:
                            break
                        opcode = int.from_bytes(data, 'big')
                        if opcode < OP_NAMES.index('disc'):
                            foobar_action(conn, OP_NAMES[opcode])
                        elif opcode == OP_NAMES.index('disc'):
                            break
                        else:
                            raise Exception('Invalid operation')
                    finally:
                        send(conn, str(OP_NAMES.index('ack')))
            print('Connection closed')
            stop_track_check()

if __name__ == '__main__':
    main()
