import sys
import os
from converter import TimeMLToColumns
#from converter import ColumnsToCAT

import sys;
reload(sys);
sys.setdefaultencoding("utf8")

#check directory existence
def ensureDir(f):
    d = os.path.dirname(f)
    if not os.path.exists(d):
        os.makedirs(d)
    
def printUsage():
    print "usage: python convertTimeMLToColumns.py dir_name [options]"
    print "   or: python convertTimeMLToColumns.py file_name [options]"
    print " "
    print "       options: -p parser_name: stanford | textpro (for tokenization and sentence splitting, default: textpro)"
    print "       options: -o output_dir_name/file_name (default: dir_path/dir_name_COL/ for directory and file_path/file_name.col for file)"

#main
if __name__ == '__main__':
    
    if len(sys.argv) < 2:
        printUsage()
    else:
        parser_name = ""
        output = ""       
        if len(sys.argv) > 2:
            for i in range(2, len(sys.argv)):
                if sys.argv[i] == "-p":
                    if i+1 < len(sys.argv): parser_name = sys.argv[i+1]
                elif sys.argv[i] == "-o":
                    if i+1 < len(sys.argv): output = sys.argv[i+1]

        if os.path.isdir(sys.argv[1]):  #input is directory name
            dirpath = sys.argv[1]
            if dirpath[-1] != "/": dirpath += "/"

            if output != "":
                output_dir_name = output
            else:
                output_dir_name = os.path.dirname(dirpath) + "_COL/"

            if output_dir_name[-1] != "/": output_dir_name += "/"
            ensureDir(output_dir_name)

            for r, d, f in os.walk(dirpath):
                for filename in f:
                    #print filename
                    if filename.endswith(".tml"):
                        filepath = os.path.join(r, filename)
                        print "Converting " + filepath + "..."
                        out_file = open(output_dir_name + os.path.basename(filepath).replace(".tml", ".col"), "w")
                        if parser_name != "": timeml_cols = TimeMLToColumns.TimeMLToColumns(filepath, parser_name)
                        else: timeml_cols = TimeMLToColumns.TimeMLToColumns(filepath)
                        out_file.write(timeml_cols.parseTimeML())
                        out_file.close()

            print "Column file(s) are saved in " + output_dir_name

        elif os.path.isfile(sys.argv[1]):   #input is file name
            print "Converting " + sys.argv[1] + "..."

            if output != "":
                out_file_name = output
            else:
                out_file_name = os.path.splitext(os.path.basename(sys.argv[1]))[0] + ".col"
            out_file = open(out_file_name, "w")

            if parser_name != "": timeml_cols = TimeMLToColumns.TimeMLToColumns(sys.argv[1], parser_name)
            else: timeml_cols = TimeMLToColumns.TimeMLToColumns(sys.argv[1])
            out_file.write(timeml_cols.parseTimeML())
            out_file.close()

            print "Column file is saved in " + out_file_name

        else:
            print "File/directory " + sys.argv[1] + " doesn't exist."
                    
