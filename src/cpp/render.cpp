#include <iostream>
#include <opencv/cv.hpp>
#include <random>
#include "me_replaymaster_ReplayMaster.h"

#define FPS 60
#define PIXEL_PER_FRAME (900 / FPS)
#define MIN_LN_TIME 150
#define BLOCK_HEIGHT 40
#define GAME_WIDTH 540
#define GAME_HEIGHT 960
#define TIME_INTERVAL (1000.0 / FPS)
#define TIME_WINDOW ((double) GAME_HEIGHT / PIXEL_PER_FRAME * TIME_INTERVAL)
#define TIME_TO_HEIGHT(t) ((int)(((double) (t)) / TIME_WINDOW * GAME_HEIGHT))

#define TYPE_IMAGE CV_8UC3

using namespace cv;
using namespace std;

struct Note {
    long timeStamp;
    int column;
    int duration;
    int judgement;

    inline long getEndTime() {
        return timeStamp + duration;
    }

    Note(long timeStamp, int column, int duration, int judgement) : timeStamp(timeStamp),
                                                                    column(column),
                                                                    duration(duration),
                                                                    judgement(judgement) {}
};

void readNote(istream &stream, vector<Note> &vector) {
    long timeStamp;
    int column;
    int duration;
    int judgement;
    stream >> timeStamp >> column >> duration >> judgement;
    vector.emplace_back(timeStamp, column, duration, judgement);
}

inline Scalar getJudgementColor(int judgement) {
    switch (judgement) {
        case 0:
            return Scalar(255, 255, 255);
        case 1:
            return Scalar(55, 210, 255);
        case 2:
            return Scalar(32, 208, 121);
        case 3:
            return Scalar(197, 104, 30);
        case 4:
            return Scalar(131, 117, 106);
        default:
            return Scalar(0, 0, 255);
    }
}

int columnWidth = 0;

int render(int beginIndex, vector<Note> &data, double time, Mat &image, bool isBase) {
    const int dataSize = data.size();
    for (int i = beginIndex; i < dataSize && data[i].timeStamp <= time; i++) {
        auto &note = data[i];
        if (time - note.timeStamp > TIME_WINDOW) {
            beginIndex = i + 1;
            continue;
        }
        int x = (int) (note.column * columnWidth);
        int y = TIME_TO_HEIGHT(time - note.timeStamp);
        int h = MIN(BLOCK_HEIGHT, TIME_TO_HEIGHT(MAX(note.duration, MIN_LN_TIME)));
        int width = columnWidth;
        if (!isBase) {
            h = 5;
            x += width / 5;
            width -= 2 * width / 5;
        }
        Scalar color = isBase ? Scalar::all(127) : getJudgementColor(note.judgement);
        rectangle(image, Point(x, y), Point(x + width, y - h),
                  color, isBase ? 1 : FILLED);
    }
    return beginIndex;
}

/*
 * Class:     me_replaymaster_ReplayMaster
 * Method:    nativeRender
 * Signature: (IILjava/lang/String;ILjava/lang/String;Ljava/lang/String;)
 *
 * external fun nativeRender(key: Int,
                              beatSize: Int, beatNotes: String,
                              replaySize: Int, replayNotes: String,
                              resultPath: String)
 */
JNIEXPORT void JNICALL Java_me_replaymaster_ReplayMaster_nativeRender
        (JNIEnv *env, jclass, jint key, jint beatSize, jstring jBeatNotes, jint replaySize,
         jstring jReplayNotes, jstring jPath) {

    auto beatNotes = env->GetStringUTFChars(jBeatNotes, JNI_FALSE);
    auto replayNotes = env->GetStringUTFChars(jReplayNotes, JNI_FALSE);
    auto path = env->GetStringUTFChars(jPath, JNI_FALSE);

    Size imageSize(GAME_WIDTH, GAME_HEIGHT);
    VideoWriter writer(string(path), VideoWriter::fourcc('P', 'I', 'M', '1'), FPS, imageSize);
    Mat image(imageSize, TYPE_IMAGE);
    Mat dark(imageSize, TYPE_IMAGE, Scalar::all(0));
    vector<Note> beats;
    vector<Note> replay;

    stringstream beatStream(beatNotes), replayStream(replayNotes);

    for (int i = 0; i < beatSize; i++) {
        readNote(beatStream, beats);
    }

    for (int i = 0; i < replaySize; i++) {
        readNote(replayStream, replay);
    }

    auto duration = beats[beatSize - 1].getEndTime() + 2000;
    cout << "duration: " << duration << "ms" << endl;
    int lastBeat = 0, lastReplay = 0;
    columnWidth = (int) ((double) GAME_WIDTH / key);

    int progress = 0;

    for (double time = 0; time < duration; time += TIME_INTERVAL) {
        dark.copyTo(image);
        if ((int) (time / duration * 100) > progress) {
            progress = (int) (time / duration * 100);
            cout << progress << "%" << endl;
        }

        lastBeat = render(lastBeat, beats, time, image, true);
        lastReplay = render(lastReplay, replay, time, image, false);

        writer.write(image);
    }

    image.release();
    writer.release();

    env->ReleaseStringUTFChars(jBeatNotes, beatNotes);
    env->ReleaseStringUTFChars(jReplayNotes, replayNotes);
    env->ReleaseStringUTFChars(jPath, path);
}

