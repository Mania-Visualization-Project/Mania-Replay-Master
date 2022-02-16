import matplotlib.pyplot as plt
from matplotlib import colors
import numpy as np
import sys
import json
import os

# path = input("Please input the judgement results path:").strip()
# if path.startswith('"'):
#     path = path[1:]
# if path.endswith('"'):
#     path = path[:-1]
# path = "G:\code\java\ManiaReplayMaster\out_test\judgement_[Crz]Caicium - Nauts - Second Run (Core Mix) [4K LN] (2021-07-24) OsuMania-1.json"
# path = r"E:\code\java\ManiaReplayMaster\out\publish\ManiaReplayMaster v2.3.2\out\judgement_4kGameBye - Various Artists - extra.Dan ~ REFORM ~ Pack [~ EXTRA-BETA ~ (Marathon)] (2022-02-02) OsuMania.json"
path = sys.argv[1]
print(path)
data_str = open(path).read()
data = json.loads(data_str)


def main(unit, click_x):
    press = {}  # key -> ([offset -> (count, [timestamps])], [offsets])
    release = {}  # key -> ([offset -> (count, [timestamps])], [offsets])

    def increase(data_dict, key, offset, timestamp):
        if key not in data_dict:
            data_dict[key] = ({}, [])
        data_dict_key = data_dict[key]
        offset_smooth = int(offset / unit) * unit
        old = data_dict_key[0].get(offset_smooth, [0, []])
        old[0] += 1
        timestamp = timestamp + 1000
        seconds = int(timestamp / 1000)
        h = int(seconds / 3600)
        m = (seconds - 3600 * h) / 60
        s = seconds % 60
        ms = timestamp % 1000
        old[1].append((timestamp, " %02d:%02d:%02d.%03d (key %d)" % (h, m, s, ms, key + 1)))
        data_dict_key[0][offset_smooth] = old
        data_dict_key[1].append(offset)

    def process(data_dict):
        offset_to_times = {}
        for k in data_dict:
            x = []
            y = []
            for offset in range(-200, 200, 1):
                if offset % unit != 0:
                    continue
                offset_smooth = int(offset / unit) * unit
                x.append(offset_smooth)
                old_times = offset_to_times.get(offset, [])

                if offset_smooth in data_dict[k][0]:
                    y.append(data_dict[k][0][offset_smooth][0])
                    old_times += data_dict[k][0][offset_smooth][1]
                else:
                    y.append(0)
                offset_to_times[offset] = old_times
            data_dict[k] = (x, y, data_dict[k][1], offset_to_times)
        for k in offset_to_times:
            offset_to_times[k] = list(map(lambda x: x[1], sorted(offset_to_times[k], key=lambda x: x[0])))
        return offset_to_times

    for action in data:
        if action['judgementStart'] != -1:
            increase(press, action['column'], action['offSetStart'], action['timeStamp'])
        if action['judgementEnd'] != -1:
            increase(release, action['column'], action['offSetEnd'], action['timeStamp'])

    press_times = process(press)
    release_times = process(release)

    plt.clf()

    def draw(data_dict, label, extra_text):
        offset_statistics = []
        max_y = 0
        for k in range(16):
            if k not in data_dict:
                continue
            rgbcolor = colors.hsv_to_rgb((k / len(data_dict), 1, 1)) * 255
            colorst = "#" + hex(int(rgbcolor[0]))[2:].zfill(2) + hex(int(rgbcolor[1]))[2:].zfill(
                2) + hex(
                int(rgbcolor[2]))[2:].zfill(2)
            plt.plot(data_dict[k][0], data_dict[k][1], label='key ' + str(k + 1), color=colorst)
            # plt.hist(data_dict[k][2], bins=200, label='key ' + str(k + 1), color=colorst)
            mean = np.mean(data_dict[k][2])
            std = np.std(data_dict[k][2])
            max_y = max(max_y, np.max(data_dict[k][1]))
            comment = ""
            if mean > 0:
                comment = "(late)"
            elif mean < 0:
                comment = "(early)"
            offset_statistics.append("key %d: %.2lf±%.2lf ms %s" % (
                k + 1,
                mean,
                std,
                comment))

        plt.grid()
        plt.text(150, 0, str(), ha='right', va='top')
        plt.xticks(fontsize=15)
        plt.yticks(fontsize=15)
        plt.xlim(-150, 150)
        plt.xlabel(label + ' offset(ms)', fontsize=15)
        plt.ylabel(r'count', fontsize=15)
        plt.title('\n'.join(offset_statistics))
        if extra_text is None or extra_text == "" or len(extra_text.split("\n")) == 1:
            plt.legend(shadow=True, fontsize=10, ncol=2)
        else:
            plt.text(-150, max_y, extra_text, ha='left', va='top')

    plt.subplot(2, 1, 1)
    click_x = round(click_x / unit) * unit if click_x is not None else None

    def get_time_list(click_x, dict_data):
        list = dict_data[click_x] if click_x is not None else []
        if len(list) == 0:
            return ""
        count = len(list)

        list = [" offset = %d ms:" % click_x] + list[:7]
        if count > 7:
            list += [" (%d more result%s...)" % (count - 7, '' if count == 8 else 's')]
        return "\n".join(list)

    draw(press, "press", get_time_list(click_x, press_times))
    plt.subplot(2, 1, 2)
    draw(release, "LN release", get_time_list(click_x, release_times))
    plt.tight_layout()
    plt.subplots_adjust(hspace=1)
    plt.draw()

unit = 2
click_x = None

def zoom_fun(event):
    global unit, click_x
    click_x = None
    if event.button == 'up':
        # deal with zoom in
        unit = unit + 1
    elif event.button == 'down':
        # deal with zoom out
        unit = max(1, unit - 1)
    main(unit, click_x)
    plt.draw()  # force re-draw

def onclick(event):
    global unit, click_x
    print('%s click: button=%d, x=%d, y=%d, xdata=%f, ydata=%f' %
          ('double' if event.dblclick else 'single', event.button,
           event.x, event.y, event.xdata, event.ydata))
    click_x = event.xdata
    main(unit, click_x)
    plt.draw()

def resize(event):
    global unit, click_x
    main(unit, click_x)
    plt.draw()

fig = plt.figure(figsize=(6, 7))
fig.canvas.mpl_connect('scroll_event', zoom_fun)
fig.canvas.mpl_connect('button_press_event', onclick)
fig.canvas.mpl_connect('resize_event', resize)
fig.canvas.set_window_title("mouse scroll: adjust smoothness. click: view timestamps.")
main(unit, click_x)
plt.show()

