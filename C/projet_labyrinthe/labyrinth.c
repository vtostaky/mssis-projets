#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <unistd.h>
#include <limits.h>
#include "labyrinth.h"

#define MASK_L 1
#define MASK_R (1 << 2)
#define MASK_D (1 << 1)
#define MASK_U (1 << 3)
#define ALL_WALLS (MASK_L | MASK_D | MASK_R | MASK_U)

#define CURRENT_POS (1 << 14)
#define TEMP_PATH (1 << 13)
#define SOL_PATH (1 << 12)

#define esc 27



void find_next_wall(unsigned short *current_wall);

void clrscreen()
{
    printf("%c[2J%c[H", esc, esc);
}

void replace()
{
    printf("%c[4l", esc);
}

/**
 * print_labyrinth(LABYRINTH lab)
 *
 * Prints a labyrinth given as a parameter
 *
 * @lab : the labyrinth to be printed
 */
void print_labyrinth(LABYRINTH lab)
{
    int x, y;

    usleep(100000);
    clrscreen();
    replace();

    /* print first line (upside border) */
    for(y = 0; y < lab.num_col; y++)
        printf("+--");
    printf("+\n");

    for(x = 0; x < lab.num_lines; x++)
    {
        /* add a | to represent the left line border at the first column */
        printf("|");
        for(y = 0; y < lab.num_col; y++)
        {
            /* print current position of the cursor, represented using a @ */
            if(lab.matrix[x][y].value & CURRENT_POS)
                printf("@ ");
            /* print cells that are part of the solution path, or cells part of the research path */
            else if((lab.matrix[x][y].value & SOL_PATH) || (lab.matrix[x][y].value & TEMP_PATH))
                printf("* ");
            /* print other cells (empty) */
            else
                printf("  ");

            /* add a | for cell vertical walls */
            if(lab.matrix[x][y].value & MASK_R)
                printf("|");
            /* if no wall, print a space */
            else
                printf(" ");
        }
        printf("\n");
        for(y = 0; y < lab.num_col; y++)
        {
            printf("+");
            /* print -- for cell horizontal walls */
            if(lab.matrix[x][y].value & MASK_D)
                printf("--");
            /* if no wall, print 2 spaces */
            else
                printf("  ");
        }
        printf("+\n");
    }
}

/**
 * load_labyrinth
 *
 * Loads a labyrinth in memory from a file, of which the descriptor is given.
 *
 * @fp : file descriptor
 *
 * return: a pointer to the created labyrinth
 */
LABYRINTH *load_labyrinth(FILE *fp)
{
    int x,y;
    int index_value;
    int ret;
    LABYRINTH *lab = malloc(sizeof(LABYRINTH));
    if(fp != NULL)
    {
        ret = fscanf(fp,"%d %d %d %d %d %d", &(lab->num_lines),
                &(lab->num_col),
                &(lab->inX),
                &(lab->inY),
                &(lab->outX),
                &(lab->outY));

        /* check that the header parameters are accurate */
        if(ret != 6 ||
            lab->num_lines <= 0 || lab->num_col <= 0 ||
            lab->num_lines > INT_MAX || lab->num_col > INT_MAX ||
            lab->inX >= lab->num_lines || lab->inY >= lab->num_col ||
            lab->outX >= lab->num_lines || lab->outY >= lab->num_col ||
            lab->inX < 0 || lab->inY < 0 ||
            lab->outX < 0 || lab->outY < 0)
        {
            printf("Problem while reading labyrinth file : non expected type of header!\n");
            goto exit_failure;
        }

        /* allocate memory for the cells matrix */
        lab->matrix = calloc(lab->num_lines, sizeof(CELL*));
        for(x = 0; x < lab->num_lines; x++)
        {
            lab->matrix[x] = calloc(lab->num_col, sizeof(CELL));
        }
        x = 0;
        while(x < lab->num_lines)
        {
            y = 0;
            do
            {
                /* check that we manage to read an integer for the current cell to be filled */
                ret = fscanf(fp, "%d", &index_value);
                if(ret != 1)
                {
                    printf("Problem while reading labyrinth file : expected an integer!\n");
                    goto exit_failure;
                }
                lab->matrix[x][y].value = (unsigned short)(index_value | (index_value << 4));
                /* will be used only in case of file usage for Breadth First Search algorithm */
                lab->matrix[x][y].x = x;
                lab->matrix[x][y].y = y;
                /* used for replay mechanism */
                lab->matrix[x][y].child = NULL;
                y++;
            }
            while(y < lab->num_col);
            x++;
        }
    }
    return lab;
exit_failure:
    free_lab(lab);
    return NULL;
}

/**
 * check_labyrinth
 *
 * Check the given labyrinth is accurate (no issue in the header or in the values)
 *
 * @lab: the labyrinth to be verified
 *
 * return: 1 if labyrinth is accurate, 0 is not.
 */
int check_labyrinth(LABYRINTH lab)
{
    int x, y;

    if(lab.matrix == NULL ||
            lab.num_lines == 0 || lab.num_col == 0 ||
            lab.inX >= lab.num_lines || lab.inY >= lab.num_col ||
            lab.outX >= lab.num_lines || lab.outY >= lab.num_col)
    {
        printf("Labyrinth attributes issue!\n");
        return 0;
    }

    for(x = 0; x < lab.num_lines; x++)
    {
        for(y = 0; y < lab.num_col; y++)
        {
            /* check upside border */
            if(x == 0 && !(lab.matrix[x][y].value & MASK_U))
            {
                printf("Upside border check failure at %d, %d\n", x, y);
                return 0;
            }

            /* check downside border */
            if(x == lab.num_lines - 1 && !(lab.matrix[x][y].value & MASK_D))
            {
                printf("Downside border check failure at %d, %d\n", x, y);
                return 0;
            }

            /* check left border */
            if(y == 0 && !(lab.matrix[x][y].value & MASK_L))
            {
                printf("Left border check failure at %d, %d\n", x, y);
                return 0;
            }

            /* check right border */
            if(y == lab.num_col - 1 && !(lab.matrix[x][y].value & MASK_R))
            {
                printf("Right border check failure at %d, %d\n", x, y);
                return 0;
            }

            /* check cells separating walls */
            if(y < lab.num_col - 1)
            {
                if(((lab.matrix[x][y].value & MASK_R) >> 2) != (lab.matrix[x][y+1].value & MASK_L))
                {
                    printf("Right/left check failure at %d, %d with %d, %d\n", x, y, lab.matrix[x][y].value, lab.matrix[x][y+1].value);
                    return 0;
                }
            }

            if(x < lab.num_lines - 1)
            {
                if(((lab.matrix[x][y].value & MASK_D) << 2) != (lab.matrix[x+1][y].value & MASK_U))
                {
                    printf("Up/down check failure at %d, %d with %d, %d\n", x, y, lab.matrix[x][y].value, lab.matrix[x+1][y].value);
                    return 0;
                }
            } 
        }
    }
    return 1;
}

/**
 * generate_labyrinth
 *
 * Randomly generate a new labyrinth, with number of lines & number of columns between 1 & 40.
 * Input & output positions are also generated randomly.
 *
 * return: the newly generated labyrinth
 */
LABYRINTH *generate_labyrinth()
{
    LABYRINTH *lab = calloc(1,sizeof(LABYRINTH));
    int x, y;
    static int first = 0;
    int index_wall_kept;
    unsigned short cell_walls[4] = {MASK_L, MASK_D, MASK_R, MASK_U};

    if (first == 0)
    {
        srand (time (NULL));
        first = 1;
    }

    /* Numbers of lines & columns have to be strictly greater than 0, otherwise there is no labyrinth */
    lab->num_lines  = rand()%40 + 1;
    lab->num_col    = rand()%40 + 1;
    lab->inX        = rand()%(lab->num_lines);
    lab->outX       = rand()%(lab->num_lines);

    if(lab->inX != 0)
        lab->inY = 0;
    else
        lab->inY = rand()%(lab->num_col);

    if(lab->outX != lab->num_lines-1)
        lab->outY = lab->num_col-1;
    else
        lab->outY = rand()%(lab->num_col);

    lab->matrix = calloc(lab->num_lines, sizeof(CELL*));
    for(x = 0; x < lab->num_lines; x++)
    {
        lab->matrix[x] = calloc(lab->num_col, sizeof(CELL));
    }

    for(x = 0; x < lab->num_lines; x++)
    {
        for(y = 0; y < lab->num_col; y++)
        {
            if(x == 0)
                lab->matrix[x][y].value |= MASK_U | (MASK_U << 4);
            if(y == 0)
                lab->matrix[x][y].value |= MASK_L | (MASK_L << 4);
            if(x == lab->num_lines -1)
                lab->matrix[x][y].value |= MASK_D | (MASK_D << 4);
            if(y == lab->num_col - 1)
                lab->matrix[x][y].value |= MASK_R | (MASK_R << 4);

            
            /*
             * Originally calloc has initialized all cells to 0,
             * so the only reason why adjacent cell wall wouldn't match,
             * is that a wall has been built in previously initialized cell
             */
            if(y > 0)
            {
                if(((lab->matrix[x][y-1].value & MASK_R) >> 2) != (lab->matrix[x][y].value & MASK_L))
                {
                    lab->matrix[x][y].value |= MASK_L | (MASK_L << 4);
                }
            }

            if(x > 0)
            {
                if(((lab->matrix[x-1][y].value & MASK_D) << 2) != (lab->matrix[x][y].value & MASK_U))
                {
                    lab->matrix[x][y].value |= MASK_U | (MASK_U << 4);
                }
            }

            /* Randomly select which well won't be destroyed */
            index_wall_kept = rand()%4;

            /* Only modify walls without constraints */
            if(cell_walls[index_wall_kept] != MASK_U && cell_walls[index_wall_kept] != MASK_L)
                lab->matrix[x][y].value |= cell_walls[index_wall_kept] | (cell_walls[index_wall_kept] << 4);

            /* Only used for BFS algorithm */
            lab->matrix[x][y].x = x;
            lab->matrix[x][y].y = y;
            /* Used for replay mechanism */
            lab->matrix[x][y].child = NULL;
        }
    } 

    return lab;
}

/**
 * free_lab
 *
 * Deallocate memory used for the given labyrinth.
 *
 * @lab : the labyrinth to be freed.
 */
void free_lab(LABYRINTH *lab)
{
    int x, y;

    if(lab != NULL)
    {
        if(lab->matrix != NULL)
        {
            for(x = 0; x < lab->num_lines; x++)
            {
                if(lab->matrix[x] != NULL)
                {
                    for(y = 0; y < lab->num_col; y++)
                    {
                        lab->matrix[x][y].child = NULL;
                    }
                    free(lab->matrix[x]);
                }
            }
            free(lab->matrix);
        }
        free(lab);
    }
}

/**
 * clear_lab
 *
 * Clear labyrinth to come back to initial state.
 * 
 * @lab : labyrinth to be cleared
 */
void clear_lab(LABYRINTH *lab)
{
    int x, y;

    if(!check_labyrinth(*lab))
        return;
    for(x = 0; x < lab->num_lines; x++)
    {
        for(y = 0; y < lab->num_col; y++)
        {
            lab->matrix[x][y].value &= ALL_WALLS;
            lab->matrix[x][y].value |= (lab->matrix[x][y].value << 4);
            lab->matrix[x][y].child = NULL;
        }
    }
}

/**
 * clear_research_markers_lab
 *
 * Clear research markers but keep solution path identifiers
 *
 * @lab : labyrinth to be cleared
 */
void clear_research_markers_lab(LABYRINTH *lab)
{
    int x, y;

    if(!check_labyrinth(*lab))
        return;
    for(x = 0; x < lab->num_lines; x++)
    {
        for(y = 0; y < lab->num_col; y++)
        {
            lab->matrix[x][y].value &= (ALL_WALLS | SOL_PATH);
        }
    }
}

/**
 * clear_all_markers_lab
 *
 * Clear all markers, only keep path chained list that contains the solution, used for replay
 *
 * @lab : labyrinth to be cleared
 */
void clear_all_markers_lab(LABYRINTH *lab)
{
    int x, y;

    if(!check_labyrinth(*lab))
        return;
    for(x = 0; x < lab->num_lines; x++)
    {
        for(y = 0; y < lab->num_col; y++)
        {
            lab->matrix[x][y].value &= ALL_WALLS;
            lab->matrix[x][y].value |= (lab->matrix[x][y].value << 4);
        }
    }
}

/**
 * find_next_wall
 *
 * For a given wall, give the next wall when reading the cell anticlockwise.
 *
 * @current_wall : the pointer to the wall to be modified.
 */
void find_next_wall(unsigned short *current_wall)
{
    if(*current_wall == 0 || *current_wall == MASK_U)
        *current_wall = MASK_L;
    else
        *current_wall = ((*current_wall) << 1);
}

/**
 * find_exit_deep_exploration
 *
 * Find the output of a labyrinth by exploring recursively each cell, looking for an output.
 *      1 - Explore deeply (right-hand algorithm) until we reach a dead-end, or the exit.
 *      2a - If a dead-end is reached, go back to latest crossroads, and then go back to step 1
 *      2b - If output is reached, then stop the research
 *      2c - If after coming back from a dead-end we arrive to the input and there is no crossroad left,
 *          then the output is unreachable and the algorithm stops.
 *
 * @lab                 : the labyrinth
 * @x                   : the input abscissa
 * @y                   : the input ordinate
 * @incoming_direction  : the incoming direction (aka the way we entered the current cell)
 *
 * return : 1 if the output has been reached, 0 if not.
 */
int find_exit_deep_exploration(LABYRINTH *lab, int x, int y, unsigned short incoming_direction)
{
    int i = 0;
    unsigned short next_wall;
    unsigned short parent_direction = incoming_direction;
    int nb_walls_to_check;
    int reached_input_while_going_back = 0;

    /* If this is the first time we enter this cell, save the entry wall */
    if((lab->matrix[x][y].value & (ALL_WALLS << 8)) == 0)
    {    
        lab->matrix[x][y].value |= (incoming_direction << 8);
    }
    /* Indicate we visited this cell */
    lab->matrix[x][y].value |= SOL_PATH;

    /* Update current position marker */
    lab->matrix[x][y].value |= CURRENT_POS;

    /* Display labyrinth */
    print_labyrinth(*lab);

    /* Remove current position marker after display */
    lab->matrix[x][y].value &= ~CURRENT_POS;

    if(x == lab->outX && y == lab->outY)
    {
        // If output is reached, stop research.
        printf("Output reached\n");
        return 1;
    }

    if((lab->matrix[x][y].value & ((ALL_WALLS - incoming_direction) << 4))
            == ((ALL_WALLS - incoming_direction) << 4))
    {
        printf("Dead-end reached at %d, %d, ", x, y);

        /* Indicate we arrived at a dead-end by closing the wall */
        lab->matrix[x][y].value |= (incoming_direction << 4);

        printf("going back\n");
        /* Go back, for each cell where there is no exit left */
        while((((lab->matrix[x][y].value & (ALL_WALLS << 8)) >> 8) |
                ((lab->matrix[x][y].value & (ALL_WALLS << 4)) >> 4)) == ALL_WALLS)
        {
            /* 
             * If we reached the labyrinth input while going back and there is no exit left,
             * stop the research.
             */
            if(x == lab->inX && y == lab->inY && reached_input_while_going_back)
            {
                printf("All paths tried : no solution found!\n");
                printf("Input %d, %d output %d, %d\n", lab->inX, lab->inY, lab->outX,lab->outY);
                return 0;
            }
            lab->matrix[x][y].value &= ~SOL_PATH;
            print_labyrinth(*lab);
            
            /* Going back to the parent, if any */
            parent_direction = (lab->matrix[x][y].value & (ALL_WALLS << 8)) >> 8;
            if(parent_direction & MASK_L)
                y--;
            if(parent_direction & MASK_R)
                y++;
            if(parent_direction & MASK_D)
                x++;
            if(parent_direction & MASK_U)
                x--;
            
            /* Check if we went back to the labyrinth input */
            if(x == lab->inX && y == lab->inY)
                reached_input_while_going_back = 1;
        }

        /* Reached latest crossroads */
        incoming_direction = (lab->matrix[x][y].value & (ALL_WALLS << 8)) >> 8;
    }

    /* In case we just started the search, consider the incoming direction is the border wall at input, if any. */
    if(incoming_direction == 0)
    {
        if(lab->inX == 0)
            incoming_direction = MASK_U;
        else if(lab->inY == 0)
            incoming_direction = MASK_L;
        else if(lab->inX == lab->num_lines - 1)
            incoming_direction = MASK_D;
        else if(lab->inY == lab->num_col - 1)
            incoming_direction = MASK_R;
    }

    /* In case we just started the search and we are not coming from a border, loop on 4 walls, else loop on 3 */
    nb_walls_to_check = (incoming_direction == 0)?4:3;
    next_wall = incoming_direction;

    /* There is at least 1 cell output : let's find & explore */
    while(i < nb_walls_to_check)
    {
        find_next_wall(&next_wall);
        if(((lab->matrix[x][y].value & next_wall) != next_wall) &&
                ((lab->matrix[x][y].value & (next_wall << 4)) != (next_wall << 4)))
        {
            /* Wall is physically and logically open, close it logically */
            lab->matrix[x][y].value |= (next_wall << 4);
            /* explore next cell */
            if(next_wall & MASK_R)
                return find_exit_deep_exploration(lab, x, y+1, MASK_L);
            if(next_wall & MASK_L)
                return find_exit_deep_exploration(lab, x, y-1, MASK_R);
            if(next_wall & MASK_U)
                return find_exit_deep_exploration(lab, x-1, y, MASK_D);
            if(next_wall & MASK_D)
                return find_exit_deep_exploration(lab, x+1, y, MASK_U);
        }
        i++;
    }
    printf("Should never be there\n");
    return 0;
}

/**
 * find_shortest_path
 *
 * Use Breadth First Search algorithm to look for the shortest path the exhaustive way
 *      1 - add the first cell to a queue
 *      2 - while there are elements in the queue
 *          a - pick the first element
 *          b - if the picked cell is the output, the research ends
 *          c - else for each adjacent cell available,
 *              if not already explored, add it to the queue
 *      3 - if the queue is empty and the output has not been reached, the research ends
 *
 * @lab     : the labyrinth
 * @queue   : the queue
 */
int find_shortest_path(LABYRINTH *lab, Queue *queue)
{
    int x, y, i;
    int nb_walls_to_check;
    unsigned short next_wall;
    CELL *current_cell;

    if(lab == NULL || queue == NULL)
        return 0;

    /* Just started search? */
    if((lab->matrix[lab->inX][lab->inY].value & TEMP_PATH) != TEMP_PATH)
    {
        /* Initialize queue with input cell */
        lab->matrix[lab->inX][lab->inY].value |= TEMP_PATH;
        add_to_queue(queue, &(lab->matrix[lab->inX][lab->inY]));
        return find_shortest_path(lab, queue);
    }
    else
    {
        /* Pick from queue until queue is empty */
        while((current_cell = pick_from_queue(queue)) != NULL)
        {
            next_wall = (current_cell->value & (ALL_WALLS << 8)) >> 8;
            x = current_cell->x;
            y = current_cell->y;
            lab->matrix[x][y].value |= TEMP_PATH;
            lab->matrix[x][y].value |= CURRENT_POS;

            print_labyrinth(*lab);
            
            lab->matrix[x][y].value &= ~CURRENT_POS;

            if(x == lab->outX && y == lab->outY)
            {
                printf("Shortest path has just been found\n");
                /* Free the remaining elements of the queue */
                while(pick_from_queue(queue) != NULL);
                return 1;
            }

            /* in case we just started the search, loop on 4 walls, else loop on 3 */
            nb_walls_to_check = (next_wall == 0)?4:3;
            i = 0;

            while(i < nb_walls_to_check)
            {
                find_next_wall(&next_wall);
                if((lab->matrix[x][y].value & next_wall) != next_wall)
                {
                    /* Child cell found */
                    if((next_wall & MASK_R) &&
                            ((lab->matrix[x][y+1].value & (ALL_WALLS << 8)) == 0))
                    {
                        /* Child does not have any parent yet */
                        lab->matrix[x][y+1].value |= MASK_L << 8;
                        add_to_queue(queue, &(lab->matrix[x][y+1])); 
                    }
                    if((next_wall & MASK_L) &&
                            ((lab->matrix[x][y-1].value & (ALL_WALLS << 8)) == 0))
                    {
                        /* Child does not have any parent yet */
                        lab->matrix[x][y-1].value |= MASK_R << 8;
                        add_to_queue(queue, &(lab->matrix[x][y-1])); 
                    }
                    if((next_wall & MASK_U) &&
                            ((lab->matrix[x-1][y].value & (ALL_WALLS << 8)) == 0))
                    {
                        /* Child does not have any parent yet */
                        lab->matrix[x-1][y].value |= MASK_D << 8;
                        add_to_queue(queue, &(lab->matrix[x-1][y])); 
                    }
                    if((next_wall & MASK_D) &&
                            ((lab->matrix[x+1][y].value & (ALL_WALLS << 8)) == 0))
                    {
                        /* Child does not have any parent yet */
                        lab->matrix[x+1][y].value |= MASK_U << 8;
                        add_to_queue(queue, &(lab->matrix[x+1][y])); 
                    }
                }
                i++;
            }
        }
    }
    printf("No solution found!\n");
    printf("Input %d, %d output %d, %d \n", lab->inX, lab->inY, lab->outX,lab->outY);
    return 0;
}

/**
 * store_path
 *
 * Construct a chained list that stores the result of the latest labyrinth exploration.
 *
 * @lab : the labyrinth
 */
void store_path(LABYRINTH *lab)
{
    int x, y;
    CELL *current_cell;
    unsigned short parent_direction;

    if(lab == NULL)
        return;

    x = lab->outX;
    y = lab->outY;
    current_cell = &(lab->matrix[x][y]);

    /* Follow parents until we reach the input, and construct the corresponding chained list */
    while(current_cell != NULL)
    {
        if((current_cell->value & TEMP_PATH) == TEMP_PATH)
            current_cell->value |= SOL_PATH;
        
        parent_direction = (current_cell->value & (ALL_WALLS << 8)) >> 8;

        if((x == lab->inX && y == lab->inY) || (parent_direction == 0))
            break;

        if(parent_direction & MASK_L)
            y--;
        if(parent_direction & MASK_R)
            y++;
        if(parent_direction & MASK_D)
            x++;
        if(parent_direction & MASK_U)
            x--;

        lab->matrix[x][y].child = current_cell;
        current_cell = &(lab->matrix[x][y]);
    }
}

/**
 * replay_latest_path
 *
 * Go over the chained list, from the labyrinth input to the output.
 *
 * @lab : the labyrinth
 */
void replay_latest_path(LABYRINTH *lab)
{
    int x, y;
    CELL *cell;

    if(!check_labyrinth(*lab))
        return;
    
    x = lab->inX;
    y = lab->inY;
    cell = &(lab->matrix[x][y]);

    while(cell != NULL)
    {
        cell->value |= SOL_PATH | CURRENT_POS;
        print_labyrinth(*lab);
        cell->value &= ~CURRENT_POS;

        cell = cell->child;
    }
}

/**
 * add_to_queue
 *
 * Add a cell element to the queue.
 *
 * @queue   : the queue
 * @cell    : the cell
 */
void add_to_queue(Queue *queue, CELL *cell)
{
    QueueObj *new = malloc(sizeof(QueueObj));
    if(queue == NULL || new == NULL)
    {
        exit(EXIT_FAILURE);
    }

    new->cell = cell;
    new->next = NULL;

    if(queue->first == NULL)
    {
        queue->first = new;
    }
    else
    {
        QueueObj *current = queue->first;
        while(current->next != NULL)
        {
            current = current->next;
        }
        current->next = new;
    }
}

/**
 * pick_from_queue
 *
 * Unqueue first element.
 *
 * @queue : the queue
 *
 * return : the first cell in the queue.
 */
CELL* pick_from_queue(Queue *queue)
{
    CELL* cell = NULL;

    if(queue == NULL)
    {
        exit(EXIT_FAILURE);
    }

    if(queue->first != NULL)
    {
        QueueObj *obj = queue->first;

        cell = obj->cell;
        queue->first = obj->next;
        free(obj);
    }

    return cell;
}
