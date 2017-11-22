#include <stdio.h>
#include <stdlib.h>
#include "labyrinth.h"

int main(int argc, char **argv)
{
    int i, res;
    int found = 0;
    int exit_request = 0;
    FILE* fp;
    LABYRINTH *mylab;
    Queue queue;
    char user_input;

    if(argc > 1)
        fp = fopen(argv[1], "r");

    mylab = load_labyrinth(fp);
    fclose(fp);
    print_labyrinth(*mylab);
    printf("Labyrinth state : %d\n",check_labyrinth(*mylab));

    while(!exit_request)
    {
        if(found == 0)
            printf("<g = generate>, <s = simple search>, <f = find shortest path>, <e = exit>\n");
        else    
            printf("<g = generate>, <s = simple search>, <f = find shortest path>, <r = replay latest>, <e = exit>\n");
 dontprintagain: 
        res = scanf("%c", &user_input);
        if(res != 1)
            goto dontprintagain;

        switch(user_input)
        {
            case 'g':
                free_lab(mylab);
                found = 0;
                mylab = generate_labyrinth();
                print_labyrinth(*mylab);
                break;
            case 's':
                clear_all_markers_lab(mylab);
                found = find_exit_deep_exploration(mylab, mylab->inX, mylab->inY, 0);
                store_path(mylab);
                break;
            case 'f':
                queue.first = NULL;
                clear_lab(mylab);
                found = find_shortest_path(mylab, &queue);
                store_path(mylab);
                clear_research_markers_lab(mylab);
                if(found)
                    print_labyrinth(*mylab);
                break;
            case 'r':
                clear_all_markers_lab(mylab);
                replay_latest_path(mylab);
                break;
            case 'e':
                free_lab(mylab);
                exit_request = 1;
                break;
            default:
                goto dontprintagain;
                break;
        }
    }

#if 0
    /* Robustness test loop over 1000 randomly generated labyrinths */ 
    for(i = 0; i < 1000; i++)
    {
        free_lab(mylab);
        mylab = generate_labyrinth();
        print_labyrinth(*mylab);
                
        clear_all_markers_lab(mylab);
        found = find_exit_deep_exploration(mylab, mylab->inX, mylab->inY, 0);
        store_path(mylab);

        clear_all_markers_lab(mylab);
        replay_latest_path(mylab);

        queue.first = NULL;
        clear_lab(mylab);
        found = find_shortest_path(mylab, &queue);
        store_path(mylab);
        clear_research_markers_lab(mylab);
        if(found)
            print_labyrinth(*mylab);
                
        clear_all_markers_lab(mylab);
        replay_latest_path(mylab);
    }
#endif
    return 0;
}
