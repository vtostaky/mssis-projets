#ifndef LABYRINTH_H
#define LABYRINTH_H

/**
 * value - the value of the cell is seen as follows in 16 bits :
 * b15 b14 b13 b12 b11 b10 b9 b8 b7 b6 b5 b4 b3 b2 b1 b0
 *      - b15 : unused
 *      - b14 : the ghost (the current position in the labyrinth)
 *      - b13 : temporary research paths during BFS algorithm
 *      - b12 : the ongoing research path (aka the solution research path)
 *      - b11 b10 b9 b8 : the parent direction
 *          (i.e. the direction where we came from when entering this cell)
 *      - b7 b6 b5 b4 : the physical & logical walls
 *          (i.e. the directions where we cannot go when looking for a cell exit,
 *          logical walls are typically paths that have already been explored)
 *      - b3 b2 b1 b0 : the physical walls, which constitute the initial state
 *          of the labyrinth.
 *
 * x, y are only used for the BFS algorithm, to keep the information of the cell
 * position, when performing queue & unqueue mechanism.
 *
 * child is used to enable replay of the latest found path, to construct a chained
 * list that will embed the solution.
 */
struct _cell
{
    unsigned short value;
    int x;
    int y;
    struct _cell *child;
};
typedef struct _cell CELL;

/**
 * num_lines : the number of lines in the labyrinth
 * num_col   : the number of columns
 * inX       : the input abscissa
 * inY       : the input ordinate
 * outX      : the output abscissa
 * outY      : the output ordinate
 * matrix    : the 2D pointed memory that convey all labyrinth cells
 */
struct _laby
{
    unsigned int num_lines;
    unsigned int num_col;
    int inX;
    int inY;
    int outX;
    int outY;
    CELL **matrix;
};

typedef struct _laby LABYRINTH;


typedef struct _queueObj QueueObj;
struct _queueObj
{
    CELL *cell;
    QueueObj *next;
};

typedef struct _queue Queue;
struct _queue
{
    QueueObj *first;
};

void print_labyrinth(LABYRINTH lab);
LABYRINTH *load_labyrinth(FILE *fp);
int check_labyrinth(LABYRINTH lab);
LABYRINTH *generate_labyrinth();

void free_lab(LABYRINTH *lab);
void clear_lab(LABYRINTH *lab);
void clear_research_markers_lab(LABYRINTH *lab);
void clear_all_markers_lab(LABYRINTH *lab);

int find_exit_deep_exploration(LABYRINTH *lab, int x, int y, unsigned short incoming_direction);
int find_shortest_path(LABYRINTH *lab, Queue* queue);
void store_path(LABYRINTH *lab);
void replay_latest_path(LABYRINTH *lab);

void add_to_queue(Queue *queue, CELL *cell);
CELL* pick_from_queue(Queue *queue);

#endif
