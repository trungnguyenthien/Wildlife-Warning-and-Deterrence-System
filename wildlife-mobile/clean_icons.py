import glob, os

res_dir = 'app/src/main/res'
for folder in glob.glob(os.path.join(res_dir, 'mipmap-*')):
    for png in [os.path.join(folder, 'ic_launcher.png'), os.path.join(folder, 'ic_launcher_round.png')]:
        if os.path.exists(png):
            try:
                os.remove(png)
                print('Removed:', png)
            except Exception as e:
                print('Error:', e)
